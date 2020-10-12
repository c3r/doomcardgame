# Master of puppets - a Doom online card game

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

## REST API

### Reset the state of the game  
Reqest: `GET` `/reset`  

Example response: 
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message" : "Game restarted"
}
```
`HTTP/1.1 200 OK`

### Deal cards for player    
Reqest: `POST` `/deal/playercards/{playerId}`  

| Param | Description 
| --- | ---
| playerId | Id of the player to deal the Item Cards to 

Example response: 
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null, 
    "responseEntity" : [ 
        { "item": { "cost": 4, "name": "Super shotgun" }, "id": 618 },
        { "item": { "cost": 6, "name": "Medkit" }, "id": 605 }
    ] 
}
```  
`HTTP/1.1 200 OK`

### Deal cards for the Puppetmaster  
Request: `POST` `/deal/puppetmaster`  
Example response:
```json
{    
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null, 
    "responseEntity": 
     [
        {
            "monster": {
                "baseInitiative": 1,
                "hp": 10,
                "cost": 1,
                "name": "Imp"
            },
            "id": 305,
            "initiativeBonus": 0,
            "initiative": 1,
            "name": "Imp",
            "type": "MONSTER"
        },
        {
            "monster": {
                "baseInitiative": 3,
                "hp": 30,
                "cost": 3,
                "name": "Baron"
            },
            "id": 321,
            "initiativeBonus": 0,
            "initiative": 3,
            "name": "Baron",
            "type": "MONSTER"
        }
    ]
}
```
`HTTP/1.1 200 OK`

### Check Player's cards
Check Player's cards  
Request: `GET` `/player/{playerId}/cards`

| Param | Description 
| --- | ---
| playerId | Id of the player to deal the Item Cards to 

Example response:
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null, 
    "responseEntity" : [
        {"item":{"cost":4,"name":"Super shotgun"},"id":618},{"item":{"cost":6,"name":"Medkit"},"id":605}
    ]
}
```
`HTTP/1.1 200 OK`

### Check Puppetmaster's cards
Check the Puppetmaster's cards (current hard)  
`GET` `/puppetmaster/cards`  
Example response:
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null, 
    "responseEntity": 
    [
        {
            "monster": {
                "baseInitiative": 1,
                "hp": 10,
                "cost": 1,
                "name": "Imp"
            },
            "id": 305,
            "initiativeBonus": 0,
            "initiative": 1,
            "name": "Imp",
            "type": "MONSTER"
        },
        {
            "monster": {
                "baseInitiative": 3,
                "hp": 30,
                "cost": 3,
                "name": "Baron"
            },
            "id": 321,
            "initiativeBonus": 0,
            "initiative": 3,
            "name": "Baron",
            "type": "MONSTER"
        }
    ]
}
```
`HTTP/1.1 200 OK`

### Deal Location Card
Deal the location card to the table  
Request: `POST` `/deal/locationcard`
Example response:
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null,
    "responseEntity": {
      "location": { "name": "Room_04" },
      "id":107
      }   
}
```
`HTTP/1.1 200 OK`

### Check location card 
Check the currently played location card laying on the table  
Request: `GET` `/locationcard`
Example response:
```json

```
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null,
    "responseEntity": {
      "location": { "name": "Room_04" },
      "id":107
      }   
}
```
`HTTP/1.1 200 OK`

### Play Monster Cards 
Play Monster Cards from Puppetmaster's hand  
Request `POST` `/play/puppetmaster/monstercards` 
 
Example request:
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null,
    "responseEntity": [ 321, 305 ]   
}
```

Example response:    
`HTTP/1.1 200 OK`

### Check monster cards
Check monster cards that were played by the Puppetmaster  
Request: `GET` `/monstercards`  

Example response:  
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null,
    "responseEntity":
        [
            {
                "monster": {
                    "baseInitiative": 1,
                    "hp": 10,
                    "cost": 1,
                    "name": "Imp"
                },
                "id": 305,
                "initiativeBonus": 0,
                "initiative": 1,
                "name": "Imp",
                "type": "MONSTER"
            },
            {
                "monster": {
                    "baseInitiative": 3,
                    "hp": 30,
                    "cost": 3,
                    "name": "Baron"
                },
                "id": 321,
                "initiativeBonus": 0,
                "initiative": 3,
                "name": "Baron",
                "type": "MONSTER"
            }
        ]
}
```
`HTTP/1.1 200 OK`

### Roll for initiative for Player
Request: `POST` `/play/player/{playerId}/roll/initiative`

| Param | Description 
| --- | ---
| playerId | Id of the player to roll for initiative

Example response:
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null,
    "responseEntity": 7   
}
```
`HTTP/1.1 200 OK`

### Roll for initiative for Monster
Request: `POST` `/play/player/{monsterId}/roll/initiative`

| Param | Description 
| --- | ---
| monsterId | Id of the monster to roll for initiative 

Example response:
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null,
    "responseEntity": 11   
}
```
`HTTP/1.1 200 OK`

### Get next Creature to play
Returns the `id` of the next creature to choose the target to attack  
Request: `GET` `/play/next`

Example response:
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null,
    "responseEntity": 1   
}
```
`HTTP/1.1 200 OK` 

### Choose target
Request the target for currently playing creature  
Request: `POST` `/play/choose_target/{targetId}`


| Param | Description 
| --- | ---
| targetId | Id of the creature that will be assigned as a target for currently playing creature

Example response:

`HTTP/1.1 200 OK`

### Roll for attack 
Roll the dice for the attack value for currently attacking creature  
Request: `POST` `/play/attack`

Example response:
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null,
    "responseEntity": 5   
}
```
`HTTP/1.1 200 OK`

### Roll for defence 
Roll the dice for the defence value for currently attacked creature  
Request: `POST` `/play/defend`

Example response:
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null,
    "responseEntity": 5   
}
```
`HTTP/1.1 200 OK`

### Deal the damage
Roll for the damage that will be dealt to the attacked creature  
Request: `POST` `/play/deal_damage`

Example response:
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null,
    "responseEntity": 5   
}
```
`HTTP/1.1 200 OK`

### Check the playing queue - list monsters  
Lists all the monsters that are actually in the playing queue  
Request: `GET` `/queue/monsters`

Example response:
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null,
    "responseEntity": [321, 305]   
}
```

`HTTP/1.1 200 OK`

### Check the playing queue - list players
Lists all the players that are actually in the playing queue  
Request: `GET` `/queue/players`

Example response:
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null,
    "responseEntity": [1, 2]   
}
```
`HTTP/1.1 200 OK`

### The current attacking Creature
Displays the current attacking Creature    
Request: `GET` `/attacker`

Example response:
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null,
    "responseEntity": {
        "id": 1,
        "name": "PLAYER1",
        "hand": [
            { "item": { "cost": 5, "name": "Shotgun" }, "id": 613 },
            { "item": { "cost": 1, "name": "Shield" }, "id": 600 }
        ],
        "type": "PLAYER",
        "target": null,
        "defence": null,
        "attack": null,
        "dead": false,
        "initiative": 6
    }
}
```
`HTTP/1.1 200 OK`

### The current defending Creature
Displays the current defending Creature    
Request: `GET` `/defender`

Example response:
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null,
    "responseEntity": {
        "id": 1,
        "name": "PLAYER1",
        "hand": [
            { "item": { "cost": 5, "name": "Shotgun" }, "id": 613 },
            { "item": { "cost": 1, "name": "Shield" }, "id": 600 }
        ],
        "type": "PLAYER",
        "target": null,
        "defence": null,
        "attack": null,
        "dead": false,
        "initiative": 6
    }
}
```
`HTTP/1.1 200 OK`

### Current attack result
Displays the current attack result    
Request: `GET` `/attack/result`

Example response:
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null,
    "responseEntity": 5
}
```
`HTTP/1.1 200 OK`

### Current defend result
Displays the current attack result    
Request: `GET` `/attack/defence`

Example response:
```json
{
    "errorCode": null,
    "errorMessage": null,
    "returnCode": null,
    "message": null,
    "responseEntity": 5
}
```
`HTTP/1.1 200 OK`