package ru.gadjini.any2any.model.bot.api.object.replykeyboard.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboardMarkup;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboardRemove;

import java.io.IOException;

/**
 * @author Ruben Bermudez
 * @version 1.0
 */
public class KeyboardDeserializer extends StdDeserializer<ReplyKeyboard> {
    private final ObjectMapper objectMapper;

    public KeyboardDeserializer() {
        this(null);
    }

    private KeyboardDeserializer(Class<?> vc) {
        super(vc);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ReplyKeyboard deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node.has("keyboard")) {
            return objectMapper.readValue(node.toString(),
                    new com.fasterxml.jackson.core.type.TypeReference<ReplyKeyboardMarkup>(){});
        }

        if (node.has("inline_keyboard")) {
            return objectMapper.readValue(node.toString(),
                    new com.fasterxml.jackson.core.type.TypeReference<InlineKeyboardMarkup>(){});
        }

        if (node.has("remove_keyboard")) {
            return objectMapper.readValue(node.toString(),
                    new com.fasterxml.jackson.core.type.TypeReference<ReplyKeyboardRemove>(){});
        }

        return null;
    }
}