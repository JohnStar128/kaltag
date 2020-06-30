# KalTag

KalTag originated from an idea proposed from [KalDraven](https://www.twitch.tv/kaldraven) where players in a server could pass around a glowing effect between each other. For a while it was mostly a joke of sorts. But I decided since I somewhat know what I'm doing now that I'd make it a reality.


# Gameplay

While the game can be started at any time, the plugin checks to ensure there's at least two players online before selecting a player to tag.

When a player is tagged, they will be glowing. They can right click other players to pass their glow onto others, tagging them.

If the tagged player leaves the server, the plugin does another player count check. If the playercount is less than two, it will wait for more players to log in before selecting another.

If the count is higher than two, it will select another player right away.


# Commands and Permissions

| Command | Permission | Info |
----------|------------| -----|
| /kaltag | kaltag.toggle| Toggles the game state
