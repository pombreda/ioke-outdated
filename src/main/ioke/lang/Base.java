/*
 * See LICENSE file in distribution for copyright and licensing information.
 */
package ioke.lang;

/**
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class Base extends IokeObject {
    public Base(Runtime runtime, String documentation) {
        super(runtime, documentation);
    }

    IokeObject allocateCopy() {
        return new Base(runtime, documentation);
    }

    public void init() {
        registerMethod(new JavaMethod(runtime, "mimic", "will return a new derivation of the receiving object. Might throw exceptions if the object is an oddball object.") {
                public IokeObject activate(Context context, Message message, IokeObject on) {
                    IokeObject clone = on.allocateCopy();
                    clone.mimics(on);
                    return clone;
                }
            });
    }
}// Base
