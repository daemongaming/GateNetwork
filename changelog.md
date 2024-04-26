# CHANGELOG

## GateNetwork version 0.2.1 (Alpha Release)

### Enhancements Made
* Commands can now be safely executed from the console, with player-only commands returning an appropriate error message.
* Languages can now be set from the config file, currently supporting both English and Spanish.
* Cleaner, sleeker code to improve performance and readability.

### Bugs Squashed
* Players should no longer be vaporized when traveling from server A to B, and then back to A, within a short period of time.

### Technical Mumbo-Jumbo
* Code clean-up for the following classes:
  * GateNetwork.java: Removed unneeded code fragments and consolidated the rest. Getters for static strings just return the string directly now instead of storing it as a global variable.
  * Network.java: No longer implements Listener, with PlayerJoinEvent listening moved to the Traveling class. Some code clean-up, too.
  * Traveling.java: Now listens for PlayerJoinEvent for teleportation, consolidating the number of Listeners registered.
  * Commands.java: More logical checks for isPlayer, and assigning variable values appropriately.
  * Messages.java: Now holds all the relevant methods and lang objects, and operates more modularly.
  * Lang files: Added messages for gate settings commands.