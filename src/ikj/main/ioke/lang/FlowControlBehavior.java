/*
 * See LICENSE file in distribution for copyright and licensing information.
 */
package ioke.lang;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashSet;

import ioke.lang.exceptions.ControlFlow;

/**
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class FlowControlBehavior {
    public static void init(IokeObject obj) throws ControlFlow {
        final Runtime runtime = obj.runtime;
        obj.setKind("DefaultBehavior FlowControl");

        obj.registerMethod(runtime.newNativeMethod("takes zero or more place and value pairs and one code argument, establishes a new lexical scope and binds the places to the values given. if the place is a simple name, it will just be created as a new binding in the lexical scope. if it is a place specification, that place will be temporarily changed - but guaranteed to be changed back after the lexical scope is finished. the let-form returns the final result of the code argument.", new NativeMethod("let") {
                private final DefaultArgumentsDefinition ARGUMENTS = DefaultArgumentsDefinition
                    .builder()
                    .withRestUnevaluated("placesAndValues")
                    .withRequiredPositionalUnevaluated("code")
                    .getArguments();

                @Override
                public DefaultArgumentsDefinition getArguments() {
                    return ARGUMENTS;
                }

                @Override
                public Object activate(IokeObject method, final IokeObject context, IokeObject message, Object on) throws ControlFlow {
                    getArguments().checkArgumentCount(context, message, on);
                    List<Object> args = message.getArguments();
                    LexicalContext lc = new LexicalContext(context.runtime, context.getRealContext(), "Let lexical activation context", message, context);
                    int ix = 0;
                    int end = args.size()-1;
                    List<Object[]> valuesToUnbind = new LinkedList<Object[]>();
                    try {
                        while(ix < end) {
                            IokeObject place = IokeObject.as(args.get(ix++), context);

                            if(Message.next(place) == null && place.getArguments().size() == 0) {
                                Object value = ((Message)IokeObject.data(message)).getEvaluatedArgument(message, ix++, context);
                                lc.setCell(Message.name(place), value);
                            } else {
                                place = Message.deepCopy(place);
                                IokeObject realPlace = place;
                                while(Message.next(realPlace) != null) {
                                    if(Message.next(Message.next(realPlace)) == null) {
                                        IokeObject temp = Message.next(realPlace);
                                        Message.setNext(realPlace, null);
                                        realPlace = temp;
                                    } else {
                                        realPlace = Message.next(realPlace);
                                    }
                                }

                                Object wherePlace = context.getRealContext();
                                if(place != realPlace) {
                                    wherePlace = Message.getEvaluatedArgument(place, context);
                                }

                                final IokeObject _realPlace = realPlace;
                                final Object _wherePlace = wherePlace;

                                Object originalValue = runtime.withReturningRescue(context, null, new RunnableWithReturnAndControlFlow() {
                                        public Object run() throws ControlFlow {
                                            return ((Message)IokeObject.data(_realPlace)).sendTo(_realPlace, context, _wherePlace);
                                        }
                                    });

                                if(realPlace.getArguments().size() != 0) {
                                    String newName = realPlace.getName() + "=";
                                    List<Object> arguments = new ArrayList<Object>(realPlace.getArguments());
                                    arguments.add(args.get(ix++));
                                    IokeObject msg = context.runtime.newMessageFrom(realPlace, newName, arguments);
                                    ((Message)IokeObject.data(msg)).sendTo(msg, context, wherePlace);
                                    valuesToUnbind.add(0, new Object[]{wherePlace, originalValue, realPlace});
                                } else {
                                    Object value = ((Message)IokeObject.data(message)).getEvaluatedArgument(message, ix++, context);
                                    IokeObject.assign(wherePlace, realPlace.getName(), value, context, message);
                                    valuesToUnbind.add(0, new Object[]{wherePlace, originalValue, realPlace});
                                }
                            }
                        }

                        return ((Message)IokeObject.data(message)).getEvaluatedArgument(message, end, lc);
                    } finally {
                        while(!valuesToUnbind.isEmpty()) {
                            try {
                                Object[] vals = valuesToUnbind.remove(0);
                                IokeObject wherePlace = IokeObject.as(vals[0], context);
                                Object value = vals[1];
                                IokeObject realPlace = IokeObject.as(vals[2], context);

                                if(realPlace.getArguments().size() != 0) {
                                    String newName = realPlace.getName() + "=";
                                    List<Object> arguments = new ArrayList<Object>(realPlace.getArguments());

                                    if(value == null) {
                                        if(newName.equals("cell=")) {
                                            ((Message)IokeObject.data(context.runtime.removeCellMessage)).sendTo(context.runtime.removeCellMessage, context, wherePlace, new ArrayList<Object>(realPlace.getArguments()));
                                        } else if(newName.equals("other:cell=")) {
                                            IokeObject msg = context.runtime.newMessage("other:removeCell!");
                                            ((Message)IokeObject.data(msg)).sendTo(msg, context, wherePlace, new ArrayList<Object>(realPlace.getArguments()));
                                        } else {
                                            arguments.add(context.runtime.createMessage(Message.wrap(context.runtime.nil)));
                                            IokeObject msg = context.runtime.newMessageFrom(realPlace, newName, arguments);
                                            ((Message)IokeObject.data(msg)).sendTo(msg, context, wherePlace);
                                        }
                                    } else {
                                        arguments.add(context.runtime.createMessage(Message.wrap(IokeObject.as(value, context))));
                                        IokeObject msg = context.runtime.newMessageFrom(realPlace, newName, arguments);
                                        ((Message)IokeObject.data(msg)).sendTo(msg, context, wherePlace);
                                    }
                                } else {
                                    if(value == null) {
                                        IokeObject.removeCell(wherePlace, context, message, realPlace.getName());
                                    } else {
                                        IokeObject.assign(wherePlace, realPlace.getName(), value, context, message);
                                    }
                                }
                            } catch(Throwable e) {}
                        }
                    }
                }
            }));

        obj.registerMethod(runtime.newNativeMethod("breaks out of the enclosing context. if an argument is supplied, this will be returned as the result of the object breaking out of", new NativeMethod("break") {
                private final DefaultArgumentsDefinition ARGUMENTS = DefaultArgumentsDefinition
                    .builder()
                    .withOptionalPositional("value", "nil")
                    .getArguments();

                @Override
                public DefaultArgumentsDefinition getArguments() {
                    return ARGUMENTS;
                }

                @Override
                public Object activate(IokeObject method, IokeObject context, IokeObject message, Object on) throws ControlFlow {
                    List<Object> args = new ArrayList<Object>();
                    getArguments().getEvaluatedArguments(context, message, on, args, new HashMap<String, Object>());

                    Object value = runtime.nil;
                    if(message.getArgumentCount() > 0) {
                        value = ((Message)IokeObject.data(message)).getEvaluatedArgument(message, 0, context);
                    }
                    throw new ControlFlow.Break(value);
                }
            }));

        obj.registerMethod(runtime.newNativeMethod("returns from the enclosing method/macro. if an argument is supplied, this will be returned as the result of the method/macro breaking out of.", new NativeMethod("return") {
                private final DefaultArgumentsDefinition ARGUMENTS = DefaultArgumentsDefinition
                    .builder()
                    .withOptionalPositional("value", "nil")
                    .getArguments();

                @Override
                public DefaultArgumentsDefinition getArguments() {
                    return ARGUMENTS;
                }

                @Override
                public Object activate(IokeObject method, IokeObject context, IokeObject message, Object on) throws ControlFlow {
                    Object value = runtime.nil;
                    List<Object> args = new ArrayList<Object>();
                    getArguments().getEvaluatedArguments(context, message, on, args, new HashMap<String, Object>());
                    if(args.size() > 0) {
                        value = args.get(0);
                    }
                    IokeObject ctx = context;
                    while(ctx instanceof LexicalContext) {
                        ctx = ((LexicalContext)ctx).surroundingContext;
                    }

                    throw new ControlFlow.Return(value, ctx);
                }
            }));

        obj.registerMethod(runtime.newNativeMethod("breaks out of the enclosing context and continues from that point again.", new NativeMethod.WithNoArguments("continue") {
                @Override
                public Object activate(IokeObject method, IokeObject context, IokeObject message, Object on) throws ControlFlow {
                    getArguments().getEvaluatedArguments(context, message, on, new ArrayList<Object>(), new HashMap<String, Object>());

                    throw new ControlFlow.Continue();
                }
            }));

        obj.registerMethod(runtime.newNativeMethod("until the first argument evaluates to something true, loops and evaluates the next argument", new NativeMethod("until") {
                private final DefaultArgumentsDefinition ARGUMENTS = DefaultArgumentsDefinition
                    .builder()
                    .withOptionalPositionalUnevaluated("condition")
                    .withRestUnevaluated("body")
                    .getArguments();

                @Override
                public DefaultArgumentsDefinition getArguments() {
                    return ARGUMENTS;
                }

                @Override
                public Object activate(IokeObject method, IokeObject context, IokeObject message, Object on) throws ControlFlow {
                    getArguments().checkArgumentCount(context, message, on);

                    if(message.getArgumentCount() == 0) {
                        return runtime.nil;
                    }

                    boolean body = message.getArgumentCount() > 1;
                    Object ret = runtime.nil;
                    boolean doAgain = false;
                    do {
                        doAgain = false;
                        try {
                            while(!IokeObject.isTrue(((Message)IokeObject.data(message)).getEvaluatedArgument(message, 0, context))) {
                                if(body) {
                                    ret = ((Message)IokeObject.data(message)).getEvaluatedArgument(message, 1, context);
                                }
                            }
                        } catch(ControlFlow.Break e) {
                            ret = e.getValue();
                        } catch(ControlFlow.Continue e) {
                            doAgain = true;
                        }
                    } while(doAgain);

                    return ret;
                }
            }));

        obj.registerMethod(runtime.newNativeMethod("while the first argument evaluates to something true, loops and evaluates the next argument", new NativeMethod("while") {
                private final DefaultArgumentsDefinition ARGUMENTS = DefaultArgumentsDefinition
                    .builder()
                    .withOptionalPositionalUnevaluated("condition")
                    .withRestUnevaluated("body")
                    .getArguments();

                @Override
                public DefaultArgumentsDefinition getArguments() {
                    return ARGUMENTS;
                }

                @Override
                public Object activate(IokeObject method, IokeObject context, IokeObject message, Object on) throws ControlFlow {
                    getArguments().checkArgumentCount(context, message, on);

                    if(message.getArgumentCount() == 0) {
                        return runtime.nil;
                    }

                    boolean body = message.getArgumentCount() > 1;
                    Object ret = runtime.nil;
                    boolean doAgain = false;
                    do {
                        doAgain = false;
                        try {
                            while(IokeObject.isTrue(((Message)IokeObject.data(message)).getEvaluatedArgument(message, 0, context))) {
                                if(body) {
                                    ret = ((Message)IokeObject.data(message)).getEvaluatedArgument(message, 1, context);
                                }
                            }
                        } catch(ControlFlow.Break e) {
                            ret = e.getValue();
                        } catch(ControlFlow.Continue e) {
                            doAgain = true;
                        }
                    } while(doAgain);

                    return ret;
                }
            }));

        obj.registerMethod(runtime.newNativeMethod("loops forever - executing it's argument over and over until interrupted in some way.", new NativeMethod("loop") {
                private final DefaultArgumentsDefinition ARGUMENTS = DefaultArgumentsDefinition
                    .builder()
                    .withRestUnevaluated("body")
                    .getArguments();

                @Override
                public DefaultArgumentsDefinition getArguments() {
                    return ARGUMENTS;
                }

                @Override
                public Object activate(IokeObject method, IokeObject context, IokeObject message, Object on) throws ControlFlow {
                    getArguments().checkArgumentCount(context, message, on);

                    if(message.getArgumentCount() > 0) {
                        while(true) {
                            try {
                                while(true) {
                                    ((Message)IokeObject.data(message)).getEvaluatedArgument(message, 0, context);
                                }
                            } catch(ControlFlow.Break e) {
                                return e.getValue();
                            } catch(ControlFlow.Continue e) {
                            }
                        }
                    } else {
                        while(true){}
                    }
                }
            }));

        obj.registerMethod(runtime.newNativeMethod("evaluates the first arguments, and then evaluates the second argument if the result was true, otherwise the last argument. returns the result of the call, or the result if it's not true.", new NativeMethod("if") {
                private final DefaultArgumentsDefinition ARGUMENTS = DefaultArgumentsDefinition
                    .builder()
                    .withRequiredPositional("condition")
                    .withOptionalPositionalUnevaluated("then")
                    .withOptionalPositionalUnevaluated("else")
                    .getArguments();

                @Override
                public DefaultArgumentsDefinition getArguments() {
                    return ARGUMENTS;
                }

                @Override
                public Object activate(IokeObject method, IokeObject context, IokeObject message, Object on) throws ControlFlow {
                    getArguments().checkArgumentCount(context, message, on);

                    Object test = ((Message)IokeObject.data(message)).getEvaluatedArgument(message, 0, context);

                    LexicalContext itContext = new LexicalContext(context.runtime, context.getRealContext(), "Lexical activation context", message, context);
                    itContext.setCell("it", test);

                    if(IokeObject.isTrue(test)) {
                        if(message.getArgumentCount() > 1) {
                            return ((Message)IokeObject.data(message)).getEvaluatedArgument(message, 1, itContext);
                        } else {
                            return test;
                        }
                    } else {
                        if(message.getArgumentCount() > 2) {
                            return ((Message)IokeObject.data(message)).getEvaluatedArgument(message, 2, itContext);
                        } else {
                            return test;
                        }
                    }
                }
            }));

        obj.registerMethod(runtime.newNativeMethod("evaluates the first arguments, and then evaluates the second argument if the result was false, otherwise the last argument. returns the result of the call, or the result if it's true.", new NativeMethod("unless") {
                private final DefaultArgumentsDefinition ARGUMENTS = DefaultArgumentsDefinition
                    .builder()
                    .withRequiredPositional("condition")
                    .withOptionalPositionalUnevaluated("then")
                    .withOptionalPositionalUnevaluated("else")
                    .getArguments();

                @Override
                public DefaultArgumentsDefinition getArguments() {
                    return ARGUMENTS;
                }

                @Override
                public Object activate(IokeObject method, IokeObject context, IokeObject message, Object on) throws ControlFlow {
                    getArguments().checkArgumentCount(context, message, on);

                    Object test = ((Message)IokeObject.data(message)).getEvaluatedArgument(message, 0, context);

                    LexicalContext itContext = new LexicalContext(context.runtime, context.getRealContext(), "Lexical activation context", message, context);
                    itContext.setCell("it", test);

                    if(IokeObject.isTrue(test)) {
                        if(message.getArgumentCount() > 2) {
                            return ((Message)IokeObject.data(message)).getEvaluatedArgument(message, 2, itContext);
                        } else {
                            return test;
                        }
                    } else {
                        if(message.getArgumentCount() > 1) {
                            return ((Message)IokeObject.data(message)).getEvaluatedArgument(message, 1, itContext);
                        } else {
                            return test;
                        }
                    }
                }
            }));

        obj.registerMethod(runtime.newNativeMethod("will execute and return the value of the first argument. after the code has run, all the remaining blocks of code are guaranteed to run in order even if a non-local flow control happens inside the main code. if any code in the ensure blocks generate a new non-local flow control, the rest of the ensure blocks in that specific ensure invocation are not guaranteed to run.", new NativeMethod("ensure") {
                private final DefaultArgumentsDefinition ARGUMENTS = DefaultArgumentsDefinition
                    .builder()
                    .withRequiredPositionalUnevaluated("code")
                    .withRestUnevaluated("ensureBlocks")
                    .getArguments();

                @Override
                public DefaultArgumentsDefinition getArguments() {
                    return ARGUMENTS;
                }

                @Override
                public Object activate(IokeObject method, final IokeObject context, IokeObject message, Object on) throws ControlFlow {
                    getArguments().checkArgumentCount(context, message, on);

                    final Runtime runtime = context.runtime;
                    List<Object> args = message.getArguments();
                    int argCount = args.size();

                    Object result = runtime.nil;

                    try {
                        IokeObject msg = IokeObject.as(args.get(0), context);
                        result = ((Message)IokeObject.data(msg)).evaluateCompleteWithoutExplicitReceiver(msg, context, context.getRealContext());
                    } finally {
                        for(Object o : args.subList(1, argCount)) {
                            IokeObject msg = IokeObject.as(o, context);
                            ((Message)IokeObject.data(msg)).evaluateCompleteWithoutExplicitReceiver(msg, context, context.getRealContext());
                        }
                    }

                    return result;
                }
            }));
    }
}
