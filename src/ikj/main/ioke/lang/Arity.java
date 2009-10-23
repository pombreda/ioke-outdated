/*
 * See LICENSE file in distribution for copyright and licensing information.
 */
package ioke.lang;

import ioke.lang.exceptions.ControlFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Arity extends IokeData {

    private enum Taking { Nothing, Everything };

    private static final IokeObject getArity(IokeObject self, Taking thing) {
        IokeObject obj = self.runtime.arity.allocateCopy(null, null);
        obj.mimicsWithoutCheck(self.runtime.arity);
        Arity arity = new Arity(thing);
        obj.setData(arity);
        return obj;
    }

    public static final IokeObject getArity(IokeObject self, DefaultArgumentsDefinition def) throws ControlFlow {
        if( def == null || def.isEmpty() ) {
            return IokeObject.as(takingNothing(self), self.runtime.arity);
        }
        IokeObject obj = self.runtime.arity.allocateCopy(null, null);
        obj.mimicsWithoutCheck(self.runtime.arity);
        Arity arity = new Arity(def);
        obj.setData(arity);
        return obj;
    }

    private DefaultArgumentsDefinition argumentsDefinition;
    private Taking taking;

    public Arity(Taking taking) {
        this.taking = taking;
    }

    public Arity(DefaultArgumentsDefinition argumentsDefinition) {
        if(argumentsDefinition == null || argumentsDefinition.isEmpty()) {
            this.taking = Taking.Nothing;
        } else {
            this.argumentsDefinition = argumentsDefinition;
        }
    }

    public static final Object takingNothing(IokeObject self) throws ControlFlow {
        return self.runtime.arity.getCell(null, null, "taking:nothing");
    }

    public static final Object takingEverything(IokeObject self) throws ControlFlow {
        return self.runtime.arity.getCell(null, null, "taking:everything");
    }

    @Override
    public void init(final IokeObject arity) throws ControlFlow {
        arity.setKind("Arity");

        arity.setCell("taking:nothing", getArity(arity, Taking.Nothing));
        arity.setCell("taking:everything", getArity(arity, Taking.Everything));

        arity.registerMethod(arity.runtime.newNativeMethod("Create an Arity object from the given messages. The list of unevaluated messages given to this method will be used as if they were the arguments part of a DefaultMethod definition.", new TypeCheckingNativeMethod("from") {
                private final TypeCheckingArgumentsDefinition ARGUMENTS = TypeCheckingArgumentsDefinition
                    .builder()
                    .receiverMustMimic(arity)
                    .withRestUnevaluated("arguments")
                    .getArguments();

                @Override
                public TypeCheckingArgumentsDefinition getArguments() {
                    return ARGUMENTS;
                }

                @Override
                public Object activate(IokeObject self, IokeObject context, IokeObject message, Object on) throws ControlFlow {
                    List<Object> args = message.getArguments();
                    if (args.size() == 0) {
                        return takingNothing(self);
                    }
                    DefaultArgumentsDefinition def = DefaultArgumentsDefinition.createFrom(args, 0, args.size(), message, on, context);
                    return getArity(self, def);
                }
            }));

        arity.registerMethod(arity.runtime.newNativeMethod("returns the names for positional arguments", new TypeCheckingNativeMethod("positionals") {

                private final TypeCheckingArgumentsDefinition ARGUMENTS = TypeCheckingArgumentsDefinition
                    .builder()
                    .withOptionalPositional("includeOptionals", "true")
                    .getArguments();

                @Override
                public TypeCheckingArgumentsDefinition getArguments() {
                    return ARGUMENTS;
                }

                @Override
                public Object activate(IokeObject method, Object on, List<Object> args, Map<String, Object> keywords, IokeObject context, IokeObject message) throws ControlFlow {
                    Arity a = (Arity) IokeObject.data(on);
                    List<Object> names = new ArrayList<Object>();
                    boolean includeOptional = args.isEmpty() ? true : IokeObject.isTrue(args.get(0));
                    if (a.argumentsDefinition != null) {
                        for(DefaultArgumentsDefinition.Argument argument : a.argumentsDefinition.getArguments()) {
                            if(argument instanceof DefaultArgumentsDefinition.KeywordArgument) { continue; }
                            if (argument instanceof DefaultArgumentsDefinition.OptionalArgument) {
                                if (includeOptional) {
                                    names.add(method.runtime.getSymbol(argument.getName()));
                                }
                            } else {
                                names.add(method.runtime.getSymbol(argument.getName()));
                            }
                        }
                    }
                    return method.runtime.newList(names);
                }
            }));

        arity.registerMethod(arity.runtime.newNativeMethod("returns a dict with default keyword values", new TypeCheckingNativeMethod.WithNoArguments("keywordDefaults", arity) {
                @Override
                public Object activate(IokeObject method, Object on, List<Object> args, Map<String, Object> keywords, IokeObject context, IokeObject message) throws ControlFlow {
                    Arity a = (Arity) IokeObject.data(on);
                    Map<Object,Object> defaults = new HashMap<Object,Object>();
                    if (a.argumentsDefinition != null) {
                        for(DefaultArgumentsDefinition.Argument arg : a.argumentsDefinition.getArguments()) {
                            if (arg instanceof DefaultArgumentsDefinition.KeywordArgument) {
                                String name = arg.getName();
                                Object sym = method.runtime.getSymbol(name);
                                Object value = ((DefaultArgumentsDefinition.KeywordArgument) arg).getDefaultValue();
                                defaults.put(sym, value);
                            }
                        }
                    }
                    return method.runtime.newDict(defaults);
                }
            }));

        arity.registerMethod(arity.runtime.newNativeMethod("returns the symbol name for the krest argument.", new TypeCheckingNativeMethod.WithNoArguments("krest", arity) {
                @Override
                public Object activate(IokeObject method, Object on, List<Object> args, Map<String, Object> keywords, IokeObject context, IokeObject message) throws ControlFlow {
                    Arity a = (Arity) IokeObject.data(on);
                    if (a.argumentsDefinition != null) {
                        String name = a.argumentsDefinition.getKrestName();
                        if (name == null) {
                            return method.runtime.nil;
                        } else {
                            return method.runtime.getSymbol(name);
                        }
                    } else {
                        return method.runtime.nil;
                    }
                }
            }));

        arity.registerMethod(arity.runtime.newNativeMethod("returns the symbol name for the rest argument.", new TypeCheckingNativeMethod.WithNoArguments("rest", arity) {
                @Override
                public Object activate(IokeObject method, Object on, List<Object> args, Map<String, Object> keywords, IokeObject context, IokeObject message) throws ControlFlow {
                    Arity a = (Arity) IokeObject.data(on);
                    if (a.argumentsDefinition != null) {
                        String name = a.argumentsDefinition.getRestName();
                        if (name == null) {
                            return method.runtime.nil;
                        } else {
                            return method.runtime.getSymbol(name);
                        }
                    } else {
                        return method.runtime.nil;
                    }
                }
            }));

        arity.registerMethod(arity.runtime.newNativeMethod("returns the text representation of this arity", new TypeCheckingNativeMethod.WithNoArguments("asText", arity) {
                @Override
                public Object activate(IokeObject method, Object on, List<Object> args, Map<String, Object> keywords, IokeObject context, IokeObject message) throws ControlFlow {
                    Arity a = (Arity) IokeObject.data(on);
                    if (a.taking == Taking.Everything) {
                        return method.runtime.newText("...");
                    } else if (a.taking == Taking.Nothing) {
                        return method.runtime.newText("");
                    }
                    return method.runtime.newText(a.argumentsDefinition.getCode(false));
                }
            }));

    }

}
