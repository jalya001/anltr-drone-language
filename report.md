## Introduction
This report goes over all my work on AeroScript through the four assignments.

## Design notes
**Precode**
Even though it was recommended to keep as close to the precode as possible, I have only selectively used parts of the precodes, since I did not always agree with the designs, and other times the precode had contradictions which were only resolvable through being selective (or writing very bad code). I will give examples of this throughout the report.

I also noticed generally the precode had some very questionable parts with no conceivable benefit to being written so. To make the code easier to work on, I have refactored some of these parts.

**Compiletime vs. Runtime**
In principle, static type-checking and AST-building are compiletime processes, and the interpreter solely works on runtime processes. Yet, in the precode, all compiletime processes are considered runtime ones, and the interpreter is responsible for building the AST, which is not its responsibility.

While the interpreter still is used for interpreting, the precode has evaluation happen within the Nodes/Statements of the AST rather than through some walking method in the interpreter, which means the code for interpretation is not only in the interpreter, but also in the AST files. This means that many parts of the AST need to hold references to the interpreter, which in turn requires the AST-builder needing to be fed the interpreter to feed a reference to those parts, which is ugly for testing (although it could be avoided, but it makes some more code ugly), and bad for maintainability. I decided to keep it this way, however, since changing it would take too long and be a major deviation from the precode. For comparison, see how the type-checker I implemented has all type-checking done through walking methods and is fully self-contained.

I also had to properly separate all evaluating out from the AST-building phase, and ensure it is only done during evaluation. This was importantly to allow the AST to be built without encountering semantic errors, which are supposed to be caught by the type-checker afterwards.

**Role of Stack & Heap**
In a lot of cases, it was possible to simply make use of Java's stack instead of our own stack e.g. for queuing Statements. Or to use Java's stack features like inserting anywhere in the stack. I once made use of some of these conveniences, but have since removed them, since I believe they go against the spirit of the assignment.

Due to the interpreter having become very long previously from also serving as AST-builder, I had separated the stack-management parts into a separate file StackManager. But after separating the AST-building parts away, it became less cluttered, and I put all the stack-managing back into the interpreter.

**Code Quality**
The quality of this code is not the greatest, but I've made an effort to ensure it is "acceptable". This means I've tried to avoid redundancies, use fewer resources, have clear separation of concerns, have decent names, decent categorization, and so on. But I don't believe good quality code was a requirement, so this has not been a high priority, and I will throughout the report (and in comments in the code) highlight some places which could be better written.

## Grammar
I translated the grammar given in the assignment specifications into ANTLR, which was expectedly rather simple. I only had to make a few changes to fit the code:

First change, was that in the assignment it was specified we should use positive integers for NUMBER, yet we're dealing with floats and negative numbers, so I implemented NUMBER such.

Second change, is over how Point is supposed to support arithmetic and NEG, but none of them are actually allowed in the grammar, where acMove, the only place using Point, takes a direct Point rather than Expression. To remedy this, I changed acMove to take Expressions instead. I have not implemented type-checking for this, because in the original grammar, the wrong types get rejected by ANTLR anyway, and... In my opinion, the grammar should further be changed to not use to/by variously, but only take an Expression that either evaluates to a Point or Number. But that would make it incompatible with your test programs, so I have not gone to that extent.

## Nodes
Initially, I implemented NumberNode and OperationNode to hold the different values. But it bugged me that the negation operation had an empty right slot that did nothing, and I also had the need to implement a way of fetching variables from heap for the code (to implement "descend to ground" by fetching the altitude), so I remade NumberNode into UnaryNode, and OperationNode into BinaryNode. Where their names say how many children the Node has. And in the case of Variable and Number, the . Implementing this required further changes throughout the code.

One thing to note is that I have not implemented Ranges outside of use in Random. This is since it is used nowhere else. But I have provided some basics to help with it throughout the code if it is to be done later.

## AST-building
Since we are using ANTLR, it is natural to build the AST by using ANTLR's visitor through the ANTLR ParseTree. This is how I have implemented it. And it constructs the tree out of Nodes and Statements. I did not however, make extensive use of visit overloading, and instead used a lot of if statements and non-overloaded versions. Perhaps the current way is suboptimal, but not by enough to warrant changing it yet in my opinion.

## Type-checking
I have put TypeChecker in /compiletime, unlike how you instructed to put it in /runtime, because static type-checking is technically not done during runtime.

When coding TypeChecker, I initially considered the ANTLR ParseTree instead of our AST, but figured that I'd have to repeat many of the same actions in type-checking as in AST-building, so I decided to use the AST, as it'd be more efficient. With the AST, type-checking was made simpler, since the types of the Nodes are already given with the enum ExpressionType, such that the type-checker can skip the identification phase, and only check for semantic errors.

For typing rules, I plainly have checks to show which types are expected, and a table to show what an arithmetic operation on two types result in. I'd say the code is fairly self-documenting, but to spell this all out:

- SPEED, DURATION, SCEND, TURN expects NUMBER
- MOVE expects NUMBER or POINT
- POINT evaluates to POINT 
- NUMBER, VARIABLE, RANDOM evaluates to NUMBER
- NEG evaluates to its child type
- PLUS, MINUS, TIMES evaluates to:
    - NUMBER x NUMBER => NUMBER
    - POINT x NUMBER => POINT
    - POINT x POINT => POINT
    - everything else is invalid

Points do not support Points inside of themselves (for now), which skips the need to implement explicitly nested types like `Point<Point<NUMBER, NUMBER>, NUMBER>`, since we don't use nested Points for anything yet.

## Actions
I implemented the Actions as instructed by doing so in a similar way to how I implemented Nodes, and I consolidated a lot of the Actions (still producing the same effect), since they were functionally very similar. However...

It's unclear what acDock is supposed to do. So I made it go back to start. And acTurn does nothing except consume battery. I believe I got confirmation these are the intended behaviors.

I slavishly followed the formulas for battery costs (which make no sense to me), but noticed the battery drains very quickly. I received confirmation this was correct, but I have to run your scripts with much more battery than 100 to finish them successfully.

## Executions & Reactions
**Explicit Modes**
There was some confusion in implementing these. For Executions, you provided MethodTable and hints that they should rather be in heap, and then more hints that neither are correct. I went through a similar process with Reactions, where different parts of the code seemed to require it be implemented differently.

Initially, I implemented both globally (that is, all of them resting in the same global storage in heap). But you later clarified they were to be implemented locally (within each Execution). For this, I made the storages local, and devised the design of "explicit modes"...

For that, there is now a ModeStack that keeps track of the Modes (alias of Execution, but we use "Mode" here specifically for when Executions are used as modes). This is such that during a reaction, the time until the reaction is O(1) (popping the stack and getting the next Mode, which is where the code the reaction points to is stored), as opposed to other implementations that might take e.g. O(n) time, which could easily take milliseconds in the real world, which is not ideal if you're about to hit an obstacle at high speed.

To switch between Modes, I made a Lambda Statement class to do so, a sort of synchronous signal (I use "signal" lightly). I could probably take some measures to ensure this is more maintainable and easier to test (like by not allowing arbitrary lambdas as input, or by getting closer to actual signals and scrapping lambdas), but is not yet essential.

This likewise changes how the REPL works. Where instead of being able to receive any message, it is only able to receive messages which the current Mode is able to receive

**Executions**
According to your ANTLR grammar, an Execution is not a Statement, but according to your precode, it is. I have chosen not to alter the ANTLR grammar, but I have made the beginnings of supporting Executions as any other Statement in the code, such that at some later time, it could be not-so-difficultly adjusted to work as such.

As a result, Executions are as of now not able to take more Executions. And as for the "Program" returned by the AST-builder, I had to treat specially, such that it is not to be able to take non-Execution Statements, and only meant to take Executions. This implementation of Executions is not very intuitive, and as such hurts maintainability, but is meant to be only liminal in a way.

**Reactions**
Some things baffled me, like why you have an EmergencyStack instead of simply using the Interpreter's stack. And what is declare for? It wasn't explained in the assignment specification, and I found no good indications in the precode. I have removed all of these.

## Errors & Error-handling
**ThrowingErrorListener**
I took the BaseErrorListener signature for syntaxError and used the singleton instantiation thing the slides seemed to be using. Not sure what changes to make. At least the program now terminates on specific bad inputs caught by the parser.

**Soft Failures**
It is possible to "soft fail". Where your syntax is wrong, and ANTLR complains about it, but the program is still run. Perhaps something could be configured to disallow running when that is the case. But I don't think it's that important.

**Prebuilt Error-handling**
Initially, it would seem Java already handles most runtime errors for you. A lot of cases of silent errors and errorless crashes are avoided, and you can simply "delegate" the error-handling to Java in those cases. The same goes for most I/O. However, Java gives verbose stack traces to where it failed in its own code rather than point to where or how it failed in the Aeroscript, so you preferably would want to catch them and give your own handling. I have done this in some places, but not most, because I'm unsure how.

As for the errors which Java doesn't handle, mostly on functionalities that aren't simple interfaces into Java, these are mostly handled by ANTLR (which is also prebuilt). And... yeah, most errors are handled prebuilt. However, some of these are only handled in runtime as opposed to being caught earlier. So I guess the type-checker serves a purpose here, being compiletime.

**Semantic Errors**
Take `random [4, 10]`, which is syntactically valid and `random [10, 4]` is also syntactically valid, yet semantically wrong. Although in this case, Java actually catches it as "IllegalArgumentException: bound must be greater than origin". But I consider this a runtime responsibility, not a compiletime one, since you don't know what the values in the range are going to be if you have variables and stuff, which is why this is not in type-checker. Aside from this, all wrong semantics that can be certainly known at compiletime should be caught by the type-checker. 

## Testing
Testing is to check if your program always does as intended (that is, on ever possible input). But it's usually not feasible to test every combination of inputs on your program, so we'll have to pick out a few cases and hope they cover everything. In my tests, I've thus tried to use every functionality in AeroScript and see if they produce the correct result, and then abused them to see if they correctly fail.

And even though I talked about compiletime vs. runtime earlier, I have not separated the compiletime parts of InterpreterTest into a CompilerTest, since there is a lot of shared logic between them, and the test file is not big enough to warrant it in my opinion. Perhaps it would be apt to rename it into something, but I've kept the name for recognizability.

Now, as for, tests I have made the following:
- TypeChecker tests. Most of the weird combinations of types are tested there.
- Node tests for odd cases like arithmetic on Points. It takes the values to verify the correctness of the operations.
- Infinite loop, since you had the script for it. I made no checks for infinite loops, since I figured they were sometimes purposeful (e.g. a patrol drone), and that running out of battery was a good enough check.
- Reactions, that tests if the right values have been set as the program is told to react.
- "faultyExpressions" on potentially (semantically and/or syntactically) problematic lines. Not all of them get caught by Java, but those who don't mostly get a syntax warning by the parser.

## Conclusion
I'd say every assignment is fulfilled by this delivery. As has been explained, there remains some room for improvement, but none of that relates much to the assignments, I believe.
