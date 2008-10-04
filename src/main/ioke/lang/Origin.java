/*
 * See LICENSE file in distribution for copyright and licensing information.
 */
package ioke.lang;

/**
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class Origin extends IokeObject {
    public Origin(Runtime runtime, String documentation) {
        super(runtime, documentation);
    }

    IokeObject allocateCopy() {
        return new Origin(runtime, documentation);
    }

    public void init() {
    }

    public String toString() {
        if(this == runtime.origin) {
            return "Origin";
        } else {
            return "#<Origin:" + System.identityHashCode(this) + ">";
        }
    }
}// Origin