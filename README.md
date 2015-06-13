#BattleShip Server Protokoll
Version 1.0

Die Protokollbeschreibung für das Projekt BattleShip umfasst eine Aufschlüsselung des typischen Ablaufes der Kommunikation zwischen Client und Server sowie eine genaue Beschreibung aller JSON-Datenpakete.

### Kommunikationsablauf
##### Lobby-Funktionalität
Beim Start vom Clienten besteht keine Verbindung zum Server. Sobald der Benutzer die Mehrspieler-Lobby öffnet, wird eine Verbindung zum Server via Websockets hergestellt und der Client meldet sich zur Lobby an.  
Client -> Server `lobbysignin`  

Der Client zeigt einen leeren Lobbybrowser an und wartet auf ansynchron ankommende Datenpakete, die dem Client die Spielerliste sowie mögliche Spielherausforderungen mitteilen.  
Server -> Client `lobbyplayerlist`  

Der Client kann geeignete Spieler herausfordern.  
Client -> Server `challengeplayer`  

Der Client kann eine gesendete Herausforderung zurückziehen.  
Client -> Server `revokechallenge`  

Der Client kann eine ankommende Herausforderung annehmen.  
Client -> Server `acceptchallenge`  

Der Client kann eine ankommende Herausforderung ablehnen.  
Client -> Server `declinechallenge`  

##### Spiel-Funktionalität
d  
d  
d  
### Beschreibung der Datenpakete
> lobbysignin

```json
{"action":"lobbysignin", "username":"Superman"}
```
Der Client teilt dem Server mit, dass er die Lobby betreten hat und unter welchem Pseudonym `username` er den anderen Spielern angezeigt werden möchte. Nachdem dieses Paket abgesendet wurde, teilt der Server dem Clienten alle Änderungen der Spielerliste und eingehenden Herausforderungen mit.

> lobbyplayerlist

```json
{"action":"lobbyplayerlist", "challengedby":"Batman","playerlist":[{"username":"Batman","userid":4,"status":"available"}, {"username":"Ironman","userid":8,"status":"notavailable"}]}
```

Der Server teilt dem Client die aktuelle Spielerliste und ggf. Herausforderung mit. Bei Ankunft eines solchen Pakets soll das Lobby-Model aktualisiert werden und die GUI neu gezeichnet werden. Das Feld `challengedby` enthält den Benutzernamen des Spielers, der den Client zu einem Spiel herausgefordert hat. Liegt keine Herausforderung vor, enthält das Feld einen leeren String.  
Die Spielerliste wird als Liste von Spielern im Feld `playerlist` übermittelt. Die einzelnen Spielerobjekte enthalten den Benutzernamen im Feld `username`, die Benutzer-ID, die zum senden von Herausforderungen benötigt wird im Feld `userid`, sowie den aktuellen Status des Spielers im Feld `status`.  
Für den Status stehen 3 Möglichkeiten zur Verfügung:  
  - `available` zeigt an, dass der Spieler bereit für eine Herausforderung ist. Die GUI sollte neben dem Spieler einen Button zum Herausfordern anzeigen.
  - `notavailable` zeigt an, dass der Spieler momentan nicht zu einem Spiel herausgefordert werden kann. Das liegt entweder daran, dass der betroffene Spieler schon eine Herausforderung bekommen hat oder weil der Client bereits eine Herausforderung gesendet hat. Es soll kein Button angezeigt werden.
  - `waitingforanswer` zeigt an, dass dieser Spieler vom Client herausgefordert worden ist und noch nicht geantwortet hat. Ein Button, um die Herausforderung zurückzuziehen, soll angezeigt werden.
 
> startgame

```json
{"action":"startgame", "opponent_name":"BigHero6"}
```
Der Server teilt dem Client mit, dass ein Spiel zustande gekommen ist. Im Feld `opponent_name` wird der Name des Gegners übermittelt. Der Client sollte die GUI von der Lobby-Ansicht zu einer leeren Spiel-Ansicht verändern. Der Name des Gegners kann bereits eingetragen sein, eine Meldung, dass das Spiel gegen XY beginnt, wäre hier ebenfalls denkbar. Der Client befindet sich im Zustand, in dem der Spieler seine Schiffe platzieren soll. Der Client sollte dies durch einen Statustext anzeigen und die Möglichkeit bieten, die vordefinierten Schiffe auf dem eigenen Spielfeld zu platzieren. Platzierte Schiffe werden dem Server mit dem Datenpaket `ship_placed` mitgeteilt. Solange nicht alle Schiffe des Spielers platziert worden sind, wird sich der Status nicht ändern.

> gamestate

```json
{"action":"gamestate", "state":"your_turn"}
```
Der Server teilt dem Client eine Änderung des aktuellen Spielzustandes mit. Mögliche Werte für den Zustand im Feld `state`:
  - `your_turn` zeigt an, dass der Server auf die Durchführung eines Zuges des Clienten wartet. Der Client sollte die Spieler-Eingabemöglichkeit aktivieren und mit einem Statustext anzeigen, dass der Spieler ein Feld auf dem gegnerischen Spielfeld zum Angriff auswählen soll.
  - `opponent_turn` zeigt an, dass aktuell auf den Spielzug des Gegners gewartet wird. Der Client sollte dies durch einen Statustext anzeigen und die Eingabemöglichkeit auf dem Spielfeld sperren.
  - `opponent_placing_ships` zeigt an, dass der Gegenspieler noch nicht alle Schiffe auf seinem Spielfeld platziert hat. Der Client sollte dies durch einen Statustext anzeigen.
 
> ship_placed

