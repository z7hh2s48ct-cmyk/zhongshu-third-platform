package cn.zszj.framework.swagger.config;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * ZS-SEC-009.B OpenAPI ID 类型同步的单元测试：验证 {@link ZszjSwaggerAutoConfiguration#idToStringSchemaConverter()}
 * 按命名约定改写 schema type，与 wire 合同（IdToStringAnnotationIntrospector）保持一致。
 *
 * <p>chain 以「返回预构造 schema 的桩」模拟 springdoc 默认解析结果（Long → integer 的
 * IntegerSchema 是 springdoc 默认 resolver 的实际产物形态），只验证本 converter 的改写逻辑。
 */
public class IdToStringSchemaConverterTest {

    private final ModelConverter converter = new ZszjSwaggerAutoConfiguration().idToStringSchemaConverter();

    /** 桩链：返回预构造 schema，模拟默认 resolver 已解析 Long → integer */
    private static Iterator<ModelConverter> chainOf(Schema<?> resolved) {
        return List.<ModelConverter>of((type, context, next) -> resolved).iterator();
    }

    private Schema<?> resolve(String propertyName, Schema<?> resolved) {
        AnnotatedType type = new AnnotatedType()
                .type(Long.class)
                .propertyName(propertyName);
        return converter.resolve(type, new StubContext(), chainOf(resolved));
    }

    @Test
    @DisplayName("id/userId 等 Long 属性 schema 改写为 string")
    public void testIdScalarRewrittenToString() {
        Schema<?> out = resolve("userId", new IntegerSchema());
        assertEquals("string", out.getType(), "Long ID 属性的 schema type 应改写为 string");
    }

    @Test
    @DisplayName("ID 集合属性：数组 items 改写为 string，外层仍为数组")
    public void testIdCollectionItemsRewritten() {
        ArraySchema array = new ArraySchema().items(new IntegerSchema());
        Schema<?> out = resolve("menuIds", array);
        assertInstanceOf(ArraySchema.class, out, "集合属性应保持数组形态");
        assertEquals("string", ((ArraySchema) out).getItems().getType(), "集合元素 schema type 应为 string");
    }

    @Test
    @DisplayName("非 ID 的 Long（count/total）保持 number 不误伤")
    public void testNonIdLongUntouched() {
        IntegerSchema integerSchema = new IntegerSchema();
        Schema<?> out = resolve("count", integerSchema);
        assertSame(integerSchema, out, "非 ID 属性应原样返回（不复制不改写）");
        assertEquals("integer", out.getType());
    }

    @Test
    @DisplayName("小写 id 结尾的非 ID 名（如 android/fluid 语义名）不受影响——大小写敏感约定")
    public void testLowercaseSuffixNotMatched() {
        IntegerSchema integerSchema = new IntegerSchema();
        Schema<?> out = resolve("android", integerSchema);
        assertSame(integerSchema, out);
        assertEquals("integer", out.getType());
    }

    @Test
    @DisplayName("链尾返回 null（无可解析 schema）时透传 null，不伪造")
    public void testNullResolvedPassesThrough() {
        AnnotatedType type = new AnnotatedType().type(Long.class).propertyName("id");
        Schema<?> out = converter.resolve(type, new StubContext(), List.<ModelConverter>of().iterator());
        assertNull(out);
    }

    /** ModelConverterContext 桩：本 converter 不读上下文，空实现即可（swagger-core 接口为 raw Schema） */
    @SuppressWarnings("rawtypes")
    private static final class StubContext implements ModelConverterContext {
        @Override
        public void defineModel(String key, Schema model) {
        }

        @Override
        public void defineModel(String key, Schema model, io.swagger.v3.core.converter.AnnotatedType type, String name) {
        }

        @Override
        public void defineModel(String key, Schema model, java.lang.reflect.Type type, String name) {
        }

        @Override
        public Schema resolve(io.swagger.v3.core.converter.AnnotatedType type) {
            return null;
        }

        @Override
        public java.util.Map<String, Schema> getDefinedModels() {
            return java.util.Collections.emptyMap();
        }

        @Override
        public java.util.Iterator<ModelConverter> getConverters() {
            return java.util.Collections.emptyIterator();
        }
    }
}
