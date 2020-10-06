# Master of puppets - a doom online card game

## Introduction
This is a simple PoC tool for designing a card game based (more or less) on Id Software's Doom (classic) mechanics. 
The app itself is maven based project made with Spring Boot. The purpose of this is to provide a "card game" engine
and an easy communication via HTTP API.

### Basic idea
The idea behind the engine is to provide a rich HTTP API for reading the actual game state which is internally represented
by a Finite State Machine with particular states:

ID| Name | Description 
--- | --- | --- 
A | **INIT**                        | Initial state of the application |
B | **DEAL_TO_PLAYERS**             | Cards are being dealt to players and the Puppetmaster |
C | **DEAL_TO_PM**                  | Monster Cards are being dealt to the Puppetmaster | 
D | **DEAL_LOCATION**               | The next Location Card is dealt to the table |
E | **PM_PLAY_MONSTERS**            | The Puppetmaster decides which monsters of his hand he wants to join the battle | 
F | **ROLL_DICE_FOR_INITIATIVE**    | Rolling dice for every Creature (Player or Monster played by the Puppetmaster) to determinate the Initiative attribute before the fight | 
G | **ATT_CHOOSE_TARGET**           | Attacker chooses his Victim. If every Monster died, throw out the Location Card, killed Monsters and go to **C** | 
H | **ATT_USE_ITEMS**               | Attacker chooses items from his inventory to use while attacking his Victim | 
I | **DEF_USE_ITEMS**               | Defender chooses items from his inventory to use while defending from the Attacker |
J | **ATT_ROLL**                    | Attacker rolling the dice to determine the damage amount |
K | **DEF_ROLL**                    | Defender rolling the dice to determine dodge / defense amount |
L | **DEAL_DMG**                    | Dealing the damage to the Victim, and goto **G** |

### TODO
- Phases G to L (A to F are implemented).
- REST API documentation
