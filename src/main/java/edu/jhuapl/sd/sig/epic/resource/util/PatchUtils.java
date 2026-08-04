/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.resource.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import javax.json.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PatchUtils
{

    /**
     * This is a method for accepting a generic collection of objects plus a JsonPatch. It applies the patch
     * to the collection and returns the resulting collection as a list.
     * 
     * @param patch
     * @param array
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> List<T> updateArrayUsingJsonPatch(JsonPatch patch, Collection<T> array, Class<T> clazz) throws Exception
    {
        // convert the array into a JSON string
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(array);

        // read the json string into a JsonArray object
        JsonReader reader = Json.createReader(new StringReader(jsonString));
        JsonArray jsonArray = reader.readArray();

        // apply the patch to the jsonArray; returns a new JsonArray of the changed objects.
        JsonArray result = patch.apply(jsonArray);
        reader.close();

        // Here we write the json array back into a string, then convert back into a list of objects
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = Json.createWriter(stringWriter);
        writer.writeArray(result);
        writer.close();
        CollectionType listType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, clazz);
        List<T> list = mapper.readValue(stringWriter.getBuffer().toString(), listType);
        stringWriter.close();
        return list;
    }
}
