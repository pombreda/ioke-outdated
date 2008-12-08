
ISpec do(
  Options = Origin mimic do(
    create = method(err, out,
      self with(errorStream: err, outStream: out, formatters: [], files: [], directories: []))
    
    parse! = method(
      hasFormat = false
      argv each(arg,
        if(arg == "-fp",
          hasFormat = true
          formatters << ISpec Formatter ProgressBarFormatter mimic,
          if(arg == "-fs",
            hasFormat = true
            formatters << ISpec Formatter SpecDocFormatter mimic,
            if(FileSystem directory?(arg),
              directories << arg,
              files << arg))))

      unless(hasFormat,
        formatters << ISpec Formatter ProgressBarFormatter mimic)
    )

    runExamples = method(
      files each(f, use(f))
      directories each(d,
        FileSystem["#{d}/**/*_spec.ik"] each(f, use(f)))

      reporter = ISpec Reporter create(self)

      reporter start(0)
      success = true
      ISpec specifications each(n,
        insideSuccess = n run(reporter)
        if(success, success = insideSuccess))

      reporter end
      reporter dump
      success
    )
  )
  
  Runner = Origin mimic do(
    registerAtExitHook = method(
      System atExit(
        unless(ISpec didRun?,
          success = ISpec run
          if(ISpec shouldExit?,
            System exit(success))))
      ISpec Runner registerAtExitHook = nil
    )

    CommandLine = Origin mimic do(
      run = method(instance_ispec_options,
        result = instance_ispec_options runExamples
        ISpec didRun? = true
        result
      )
    )

    OptionParser = Origin mimic do(
      create = method(err, out,
        newOP = self mimic
        newOP errorStream = err
        newOP outStream = out
        newOP options = ISpec Options create(newOP errorStream, newOP outStream)
        newOP banner = "Usage: ispec (FILE|DIRECTORY|GLOB)+ [options]"
        newOP)

      order! = method(argv,
        @argv = argv
        options argv = argv mimic
        options parse!
        options)
    )
  )

  runTest = method(
    "runs a specific test in the given describe context",
    context, name, code, reporter,

    newContext = context mimic
    newContext fullDescription = "#{newContext fullName} #{name}"
    newContext description = name
    newContext code = code

    executionError = nil

    reporter exampleStarted(newContext)

    bind(
      rescue(Ground Condition, 
        fn(c, executionError ||= c. "gah: got: #{c}")),
      handle(ISpec Condition, 
        fn(c, c describeContext = newContext)),
      if(code, 
        ;; don't evaluate directly, instead send it to a macro on the newContext, which can give it a real back trace context
        code evaluateOn(newContext, newContext),

        error!(ISpec ExamplePending, text: "Not Yet Implemented")))

    reporter exampleFinished(newContext, executionError)

    (executionError nil?) || (executionError mimics?(ExamplePending))
  )

  didRun? = false
  shouldExit? = true

  run = method(
    "runs all the defined descriptions and specs",

    if(didRun?, return(true))
    result = ispec_options runExamples
    self didRun? = true
    result)
)
