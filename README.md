# GateNetwork

## Info

A Minecraft plugin for **custom multiworld portals** with support for **multiple servers**.
 
**Supports:**
 * **Latest:** Minecraft 1.20
 * **Previous:** Minecraft 1.20

**Requires:**
 * A **Minecraft server**, running Bukkit/Spigot/Paper/etc.
 * A **MySQL database**, for data storage and synchronization.
 
**Recommended:**
 * A **BungeeCord server network**, for multiserver teleportation
 
**Resources:**
* **Minecraft**
    * Server address: *mc.gatenet.work*

* **Discord**
    * Link: *discord.gatenet.work*
    * Invite code: *KMvFm9arvy*

* **Video trailer**
    * *Coming soon!*

## Commands

*Note: Most commands are player only, which means they should be used in-game and not in the console. Support for console commands will be added in an upcoming version.*

**/gate**
  * **build**
    * *Builds a default structure for you in the world which can be used to set a new gate.*
    * Note: This is just the structure; to make a gate functional, use the "/gate new" command.
  * **new**
    * *Sets the gate for the world with your location as the center-bottom block of the ring.*
    * Note: In the default structure, it is the obsidian block directly adjacent to the block the trapdoor is on.
  * **delete**
    * *Permanently removes the gate info for the world the command is used in.*
    * Note: This is permanent.
  * **set**
    * *Sets the setting for a world's gate to the value input. Options: name, pointoforigin, address*
    * Usage: ``/command (world) (setting) (value)``
    * Example: ``/gate set world name World_Name`` ( *or* ) ``/gate set world pointoforigin HEART_POTTERY_SHERD`` ( *or* ) ``/gate set world address 1,2,3,5,6,7``
    * Note: The name setting only accepts a single value; the pointoforigin setting only accepts item IDs; and the address setting only accepts the numbers 1-43, with values 0, 4, 8, 13, 22, & 36 being reserved. 
  * **dial**
    * *Establishes a wormhole connection between your world's gate (or a specified world) and a destination world's gate.*
    * Usage: ``/command (from_world_OPT) (to_world)``
    * Example: ``/gate dial world`` ( *or* ) ``/gate dial world world_nether``
    * Note: Dialing a gate by command follows the same rules as trying to dial an address in the dialing device.
  * **info**
    * *Displays information about a world's gate.*
    * Usage: ``/command (world)``
    * Example: ``/gate info world``
  * **list**
    * *Lists all gates in the server network.*
  * **toggle**
    * *Toggles the specified plugin configuration option between on/off (true/false). Options: silent_mode, debug_mode, easy_mode*
    * Usage: ``/command (option) (setting)``
    * Example: ``/gate toggle easy_mode true``
    * Note: These options are still being worked on and more will be added in upcoming versions (such as op requirements, permissions, etc.)

## How to use

**How do gates work?**

*One Gate Rule*: 
* There is only one gate per world.

*One Way Rule*: 
* A gate can only be connected to one other gate at a time.
* A wormhole can only be traveled from the originating gate to the destination gate.
    * *Tip*: Any player going through a destination gate will be instantly vaporized.
    * *Tip*: The "easy_mode" setting can be toggled on to allow omni-directional gate travel.
    
*Wormhole Duration Rule*:
* Gates remain open for roughly 32 seconds IRL time, equating to 38 minutes of Minecraft time, after which they automatically close.
* A wormhole connection can be closed manually when the dialer interacts with the dialing device for either gate, or when anyone interacts with the dial for the originating gate.
    * *Tip*: The "easy_mode" setting can be toggled on to allow gates to pass the maximum wormhole time threshold.

**How to dial another gate?**

To connect two gates (establish a wormhole), interact with your gate's dialing device and do the following:

> **1.** Select the six symbols corresponding to your destination's address.

> **2.** Select the "point of origin" symbol, directly above the middle red button.

> **3.** Select the middle red button to attempt to dial the destination gate.

If the dial is successful, a wormhole will be established. If the other gate is busy or otherwise unreachable, it will fail to connect.

**How do I keep my inventory, etc., across servers?**

**ServerSync**: ``https://github.com/daemongaming/ServerSync``

Each server on BungeeCord has its own player data, which means inventories and such will be different when traveling to a different server.

The solution to this issue is to use a plugin to synchronize the player data between servers. Unfortunately, I could not find many options for the newest versions of MC.

So I built a separate plugin called *ServerSync* to handle player data synchronization between Minecraft servers. It is similarly available for free and the code is available on GitHub.

I did not include server syncing in GateNetwork because admins can use either plugin for their respective purposes without requiring the other. Cheers!

## Roadmap

Support for the following is on the list for upcoming versions:

* **Entity transportation**, e.g. mobs, items, and more
* **Better dialing effects**, e.g. wormhole activation, chevron locking, the whoosh effect, etc.
* **Momentum & relative location preservation**, so you come out the same way you went in
* **Optimization**, to limit resource usage, increase speed & accuracy, and improve security

## Credits

Developer: **krakenmyboy**

Alpha Tester: **LeWizardofweed**

*Special thanks* to **Dinnerbone** for the original inspiration

---

*If you immediately know the candlelight is fire, then the meal was cooked a long time ago.*

---