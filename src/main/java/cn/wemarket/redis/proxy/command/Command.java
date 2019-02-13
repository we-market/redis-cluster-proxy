package cn.wemarket.redis.proxy.command;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class Command {
    public static final byte[] EMPTY_BYTES = new byte[0];

    private final Object name;
    private final Object[] objects;
    private final boolean inline;

    public Command(Object[] objects) {
        this(null, objects, false);
    }

    public Command(Object[] objects, boolean inline) {
        this(null, objects, inline);
    }

    private Command(Object name, Object[] objects, boolean inline) {
        this.name = name;
        this.objects = objects;
        this.inline = inline;
    }

    public byte[] getName() {
        if (name != null)
            return getBytes(name);
        return getBytes(objects[0]);
    }

    public boolean isInline() {
        return inline;
    }

    private byte[] getBytes(Object object) {
        byte[] argument;
        if (object == null) {
            argument = EMPTY_BYTES;
        } else if (object instanceof byte[]) {
            argument = (byte[]) object;
        } else if (object instanceof ByteBuf) {
            argument = ((ByteBuf) object).array();
        } else if (object instanceof String) {
            argument = ((String) object).getBytes(StandardCharsets.UTF_8);
        } else {
            argument = object.toString().getBytes(StandardCharsets.UTF_8);
        }
        return argument;
    }

    public void toArguments(Object[] arguments, Class<?>[] types) {
        for (int position = 0; position < types.length; position++) {
            if (position >= arguments.length) {
                throw new IllegalArgumentException(
                        "wrong number of arguments for '"
                                + new String(getName()) + "' command");
            }
            if (objects.length - 1 > position) {
                arguments[position] = objects[1 + position];
            }
        }
    }

}
