package com.vpolosov.trainee.mergexml.validators;

import com.vpolosov.trainee.mergexml.aspect.Loggable;
import com.vpolosov.trainee.mergexml.config.ConfigProperties;
import com.vpolosov.trainee.mergexml.handler.exception.InvalidCurrencyCodeValueException;
import com.vpolosov.trainee.mergexml.utils.DocumentUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.function.Predicate;

import static com.vpolosov.trainee.mergexml.utils.Constant.FIRST_ELEMENT;
import static com.vpolosov.trainee.mergexml.utils.XmlTags.CURRCODE;

/**
 * Валидация валюты.
 *
 * @author Daria Koval
 */
@Component
@RequiredArgsConstructor
public class CurrentCodeValidator implements Predicate<File> {

    /**
     * Свойства приложения.
     */
    private final ConfigProperties configProperties;

    /**
     * Вспомогательный класс для работы с {@link Document}.
     */
    private final DocumentUtil documentUtil;

    /**
     * {@inheritDoc}
     *
     * @throws InvalidCurrencyCodeValueException когда значение кода валюты не соответствует.
     */
    @Loggable
    @Override
    public boolean test(File file) {
        Document document = documentUtil.parse(file);
        NodeList nodeListWithCurrCode = document.getElementsByTagName(CURRCODE);
        String currCode = nodeListWithCurrCode.item(FIRST_ELEMENT).getTextContent();
        var validCurrCode = String.valueOf(configProperties.getCurrencyCode());
        if (!currCode.equals(validCurrCode)) {
            throw new InvalidCurrencyCodeValueException("Допустимое значение кода валюты " + validCurrCode);
        }
        return true;
    }
}
