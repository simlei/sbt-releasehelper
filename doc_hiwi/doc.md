Commands may control execution order

val appendCommand: State => State =
  (state: State) =>
    state.copy(remainingCommands = state.remainingCommands :+ "cleanup")
    

Example: Workflow for merging WIP into develop, then testing, 
then merging devlop into master

What if you are already on develop? 
 - Fail?
 - Skip that step silently?
 - Ask interactively?

And is the answer to this question the same for any other case like that? (No.)

Step:
 - Signature (whitebox-ish)
   * am I in the intended git branch?
   * is the branch clean (and what does that mean? unstaged files?)
   * will the merge operation succeed? (product+ of the above probably)
   * are tests satisfied?
   * etc etc
 - Work (just the blackbox)
 
Signature produces work promise
promise can be fulfilled (promise.fulfill(state), may fail unexpectedly -- util.Try?)

Precondition can "dry-run" -- work on promises but not yet fulfill them
That may not work (undecidedness):
 - 


Model for dealing with this kind of undecidedness:
 - check and actual work separated
    - Check: "Can work be executed? ()"
 - check "produces" right 
