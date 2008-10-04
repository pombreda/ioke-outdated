/*
 * See LICENSE file in distribution for copyright and licensing information.
 */
package ioke.lang;

import java.util.List;

/**
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class Method extends IokeObject {
    String name;
    private Context context;

    public Method(Runtime runtime, String name, String documentation) {
        super(runtime, documentation);
        this.name = name;
    }

    public Method(Runtime runtime, Context context) {
        this(runtime, null, null);

        if(runtime.method != null && this.getClass() == Method.class) {
            this.mimics(runtime.method);
        }

        this.context = context;
    }

    public void init() {
        registerMethod(new JavaMethod(runtime, "name", "returns the name of the method") {
                public IokeObject activate(Context context, Message message, IokeObject on) {
                    return new Text(runtime, ((Method)on).name);
                }
            });
        registerMethod(new JavaMethod(runtime, "call", "activates this method with the arguments given to call") {
                public IokeObject activate(Context context, Message message, IokeObject on) {
                    return ((Method)on).activate(context, message, context.ground);
                }
            });
    }

    public String getName() {
        return name;
    }

    public boolean isActivatable() {
        return true;
    }

    public IokeObject activate(Context context, Message message, IokeObject on) {
        return runtime.nil;
    }

    public String toString() {
        if(this == runtime.method) {
            return "Method-origin";
        }
        return "Method<" + name + ">";
    }
}// Method