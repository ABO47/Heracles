package earth.terrarium.heracles.api.quests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import earth.terrarium.heracles.common.utils.ModUtils;
import org.joml.Vector2i;

import java.util.Optional;

public record GroupDisplay(
    String id,
    Vector2i position,
    float nodeScale,
    String canvasBackground,
    String groupCardTexture
) {

    public static final ByteCodec<GroupDisplay> BYTE_CODEC = ObjectByteCodec.create(
        ByteCodec.STRING.fieldOf(GroupDisplay::id),
        ModUtils.VECTOR2I_BYTE_CODEC.fieldOf(GroupDisplay::position),
        ByteCodec.FLOAT.fieldOf(GroupDisplay::nodeScale),
        ByteCodec.STRING.fieldOf(GroupDisplay::canvasBackground),
        ByteCodec.STRING.fieldOf(GroupDisplay::groupCardTexture),
        GroupDisplay::new
    );

    public static Codec<GroupDisplay> codec(String id) {
        return RecordCodecBuilder.create(instance -> instance.group(
            RecordCodecBuilder.point(id),
            ModUtils.VECTOR2I.fieldOf("position").orElse(new Vector2i()).forGetter(GroupDisplay::position),
            Codec.FLOAT.fieldOf("node_scale").orElse(1.0f).forGetter(GroupDisplay::nodeScale),
            Codec.STRING.optionalFieldOf("canvas_background").forGetter(display -> Optional.empty()),
            Codec.STRING.optionalFieldOf("group_card_texture").forGetter(display -> Optional.empty())
        ).apply(instance, (fixedId, position, nodeScale, ignoredBackground, ignoredTexture) -> new GroupDisplay(fixedId, position, nodeScale, "", "")));
    }

    public GroupDisplay(String id, Vector2i position) {
        this(id, position, 1.0f, "", "");
    }

    public static GroupDisplay createDefault() {
        return new GroupDisplay("Main", new Vector2i());
    }

    public static GroupDisplay create(String id) {
        return new GroupDisplay(id, new Vector2i());
    }

    public GroupDisplay withPosition(Vector2i pos) {
        return new GroupDisplay(this.id, new Vector2i(pos), this.nodeScale, this.canvasBackground, this.groupCardTexture);
    }

    public GroupDisplay withId(String id) {
        return new GroupDisplay(id, new Vector2i(this.position), this.nodeScale, this.canvasBackground, this.groupCardTexture);
    }

    public GroupDisplay withNodeScale(float scale) {
        return new GroupDisplay(this.id, new Vector2i(this.position), scale, this.canvasBackground, this.groupCardTexture);
    }

    public GroupDisplay withCanvasBackground(String value) {
        return new GroupDisplay(this.id, new Vector2i(this.position), this.nodeScale, "", this.groupCardTexture);
    }

    public GroupDisplay withGroupCardTexture(String value) {
        return new GroupDisplay(this.id, new Vector2i(this.position), this.nodeScale, this.canvasBackground, "");
    }
}
