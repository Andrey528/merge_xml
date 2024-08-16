package com.vpolosov.trainee.mergexml.validators;

import com.vpolosov.trainee.mergexml.aspect.Loggable;
import com.vpolosov.trainee.mergexml.handler.exception.DuplicationProcessingException;
import com.vpolosov.trainee.mergexml.model.ValidationProcess;
import com.vpolosov.trainee.mergexml.service.HistoryService;
import com.vpolosov.trainee.mergexml.service.specification.HistorySpecifications;
import com.vpolosov.trainee.mergexml.utils.DocumentUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.vpolosov.trainee.mergexml.utils.XmlTags.DOCREF;

/**
 * Проверка документа в истории совершённых платежей.
 *
 * @author Ali Takushinov
 * @author Andrei Stalybka
 * @author Maksim Litvinenko
 */
@Component
@RequiredArgsConstructor
public class CheckDocumentInHistory implements Predicate<Document> {

    /**
     * Сервис хранения истории объединённых платежей.
     */
    private final HistoryService historyService;

    /**
     * Логирование дублирующих данных.
     */
    private final Logger loggerForDouble;

    /**
     * Вспомогательный класс для работы с {@link Document}.
     */
    private final DocumentUtil documentUtil;

    /**
     * {@inheritDoc}
     *
     * @throws DuplicationProcessingException обнаружен платёж который уже есть в истории объединенных платежей.
     */
    @Loggable
    @Override
    public boolean test(Document document) {
        Map<String, String> docRefAndFileNameFromHistory = getLoadDateToBDFromHistory(document);
        if (!docRefAndFileNameFromHistory.isEmpty()) {
            StringBuilder message = new StringBuilder();
            for (var entry : docRefAndFileNameFromHistory.entrySet()) {
                message.append("В файле ")
                    .append(entry.getValue())
                    .append(" найден платеж, который уже был загружен ранее ")
                    .append(entry.getKey())
                    .append(";");
            }
            throw new DuplicationProcessingException(message.toString());
        }
        return true;
    }

    /**
     * Возвращает историю совершённых платежей.
     *
     * @param document документ с информацией о платеже.
     * @return историю платежей по переданному документу, иначе пустое значение.
     */
    @Loggable
    private Map<String, String> getLoadDateToBDFromHistory(Document document) {
        Map<String, String> docRefsAndFileNames = new HashMap<>();
        Specification<ValidationProcess> spec = Specification.where(null);

        String docRef = documentUtil.getValueByTagName(document, DOCREF);
        docRefsAndFileNames.put(docRef, documentUtil.getFileName(document));
        spec = spec.or(HistorySpecifications.docRefEquals(docRef));

        List<ValidationProcess> histories = historyService.getHistoryListBySpec(spec);
        return histories.stream()
            .peek(history -> loggerForDouble.info("Документ с номером {} из файла {} уже был загружен {}",
                history.getDocRef(),
                docRefsAndFileNames.get(history.getDocRef()),
                history.getDateTimeUpload()))
            .collect(Collectors.toMap(
                history -> history.getDateTimeUpload().toString(),
                history -> docRefsAndFileNames.get(history.getDocRef())
            ));
    }
}
