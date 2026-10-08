package es.elecciones.config;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Hace que el OpenAPI describa los records tal como los serializa Jackson: todas las claves van
 * siempre presentes (también las null), así que todos los componentes son {@code required}; los
 * anotados con {@link Nullable} admiten además {@code null}.
 *
 * <p>Sin esto springdoc marca todo como opcional y los tipos generados para el frontend
 * ({@code npm run gen:api}) no sirven. El {@code @Nullable} de JSpecify es TYPE_USE, por eso no
 * aparece entre las anotaciones que ve swagger-core y se lee del propio {@link RecordComponent}.
 *
 * <p>springdoc registra automáticamente los beans de tipo {@link ModelConverter}.
 */
@Component
class NulabilidadRecords implements ModelConverter {

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        Schema<?> schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        if (schema == null) {
            return null;
        }
        // swagger-core usa Jackson 2 internamente; aquí solo para obtener la clase de un tipo genérico.
        Class<?> clase = Json.mapper().constructType(type.getType()).getRawClass();
        if (!clase.isRecord()) {
            return schema;
        }
        Schema<?> modelo = schema.get$ref() != null
                ? context.getDefinedModels().get(schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1))
                : schema;
        if (modelo == null || modelo.getProperties() == null) {
            return schema;
        }
        List<String> obligatorios = new ArrayList<>();
        for (RecordComponent componente : clase.getRecordComponents()) {
            Schema<?> propiedad = modelo.getProperties().get(componente.getName());
            if (propiedad == null) {
                continue;
            }
            obligatorios.add(componente.getName());
            if (componente.getAnnotatedType().isAnnotationPresent(Nullable.class)) {
                aceptarNull(modelo.getProperties(), componente.getName(), propiedad);
            }
        }
        modelo.setRequired(obligatorios);
        return schema;
    }

    /** OpenAPI 3.1: {@code type: [X, "null"]}; si la propiedad es una referencia, {@code anyOf}. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void aceptarNull(Map<String, Schema> propiedades, String nombre, Schema<?> propiedad) {
        if (propiedad.get$ref() != null) {
            Schema<?> nulo = new Schema<>();
            nulo.setTypes(Set.of("null"));
            propiedades.put(nombre, new Schema<>().anyOf(List.of(new Schema<>().$ref(propiedad.get$ref()), nulo)));
        } else if (propiedad.getTypes() != null) {
            propiedad.addType("null");
        }
    }
}
