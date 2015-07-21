#BattleShip Server Protokoll
Version 1.1, Stand 27.06.2015 23:00  
Kontakt bei Rückfragen und Fehlern: `nils.sonemann@gmail.com` oder `jp.lange3@gmail.com`
 
Die Protokollbeschreibung für das Projekt BattleShip umfasst eine Aufschlüsselung des typischen Ablaufes der Kommunikation zwischen Client und Server sowie eine genaue Beschreibung aller JSON-Datenpakete.  
Die Kommunikation läuft Eventbasiert ab. Das bedeutet, dass es keine direkten Antworten auf Datenpakete gibt, sondern nach einer Aktion ein neues Event ausgelöst wird, auf das gesondert reagiert werden muss.  
Sollte ein Befehl nicht korrekt übermittelt werden, reagiert der Server darauf im Regelfall nicht und schickt euch ein Error-Event zurück, welches ihr für die Fehlersuche zur Hilfe nehmen könnt (siehe `error`).
 
### Regelset
 
  - Ein BattleShip-Spiel besteht aus 2 Spielern
  - Jeder Spieler besitzt ein Spielfeld der Größe 10 x 10 Felder
  - Die Felder sind entlang der X-Achse mit Zahlen von 1 bis 10 nummeriert, auf der Y-Achse mit den Buchstaben von A bis J benannt
  - Beide Spieler besitzen je 10 Schiffe mit eindeutiger Typkennzeichnung, genauer: 1x BattleShip (B1), 2x Cruiser (C1,C2), 3x Destroyer (D1,D2,D3) und 4x Submarine (S1,S2,S3,S4)
  - Die Außmaße der Schiffe sind wie folgt: BattleShip - 5 Kästchen, Cruiser - 4 Kästchen, Destroyer - 3 Kästchen, Submarine - 2 Kästchen
  - Die Schiffe dürfen sich zur Platzierung weder direkt noch diagonal berühren
  - Schiffe können vertikal und horizontal platziert werden
  - Nach Abschluss der Platzierung wird abwechselnd auf das gegnerische Feld geschossen, wird ein Feld getroffen, auf dem sich ein Schiff befindet, so ist dieser Teil des Schiffes getroffen. Sind alle Teile eines Schiffes getroffen, gilt das Schiff als zerstört / versenkt.
  - Sind alle Schiffe eines Spielers zerstört, hat der Gegenspieler gewonnen
  
 
### Kommunikationsablauf
Die Kommunikation erfolgt via Websockets, welche in beiden Richtungen JSON-Pakete enthalten. Zum dekodieren der JSON-Pakete bietet es sich an, eine Library, wie etwa gson, zu verwenden. Alle JSON-Pakete besitzen ein Feld `action`, in dem ein String beschreibt, um was für eine Art Paket es sich handelt. Eine Übersicht aller Strings, welche im Feld `action` vorkommen können, ist im Folgenden dargestellt. Danach ist eine detaillierte Erklärung der einzelnen Datenpakete zu finden.
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
Ein Mehrspieler-Spiel ist zustande gekommen, wenn entweder ein Spieler die Herausforderung eines anderes angenommen hat, oder sich zwei Spieler gegenseitig herausgefordert haben. Erst wenn dieses Paket beim Clienten eingetroffen ist, soll die View auf das Spielfeld geändert werden.  
Server -> Client `startgame`  
 
Der Spieler hat ein Schiff auf dem eigenen Spielfeld platziert  
Client -> Server `ship_placed`  
 
An einem der beiden Spielfelder hat sich etwas geändert (Entweder durch Schiffsplatzierung oder Angriff). Dieses Paket wird mindestens einmal pro Zug verschickt.  
Server -> Client `field_update`  
 
Der Spieler hat einen Angriff auf ein bestimmtes gegnerisches Feld initiiert  
Client -> Server `attack`  
 
Das Spiel ist vorbei (Sieg, Niederlage oder ein Spieler verlässt das Spiel)  
Server -> Client `gameover`  
 
Der Spieler sendet eine Chat-Nachricht an seinen Gegenspieler  
Client -> Server `send_chat`  
 
Der Server leitet dem Spieler eine Chat-Nachricht des Gegenspielers weiter  
Server -> Client `chat_msg`  
 
Der Server sendet dem Client eine Log-Nachricht  
Server -> Client `log_msg`  
 
##### Zusätzlich
Sollte der Client dem Server ein fehlerhaftes Datenpaket senden, wird dieses ignoriert und ein `error` Datenpaket zum Debugging gesendet  
Server -> Client `error`  
 
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

> challengeplayer

```json
{"action":"challengeplayer", "userid":4}
```

Der Client teilt dem Server mit, dass der Spieler eine Spiel-Herausforderung an einen anderen Spieler senden möchte. Das Feld `userid` beinhaltet hierbei die Spieler-ID, des herauszufordernden Spielers, die aus dem `lobbyplayerlist`-Paket entnommen werden kann.

> revokechallenge

```json
{"action":"revokechallenge"}
```

Der Client teilt dem Server mit, dass der Spieler seine aktuelle Spiel-Herausforderung zurückziehen möchte. Der Server antwortet hierdrauf mit einem `lobbyplayerlist`-Paket, welches anzeigt, dass der Spieler nun wieder Herausforderungen senden kann.

> acceptchallenge

```json
{"action":"acceptchallenge"}
```

Der Client teilt dem Server mit, dass der Spieler die vorliegende Spiel-Herausforderung angenommen hat. Da im `lobbyplayerlist`-Paket jeweils bereits der Name des herausfordernden Spielers steht, muss der Spieler jetzt nicht mehr explizit erwähnt werden. Der Server antwortet im Erfolgsfall mit einem `startgame`-Paket, welches signalisiert, dass das Spiel zustande gekommen ist. Vor Ankunft des `startgame`-Pakets soll die UI noch nicht gewechselt werden.

> declinechallenge

```json
{"action":"declinechallenge"}
```

Der Client teilt dem Server mit, dass der Spieler die vorliegende Spiel-Herausforderung abgelehnt hat. Der Server antwortet hierdrauf mit einem aktualisierten `lobbyplayerlist`-Paket.

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
 
```json
{"action":"ship_placed", "shiptype":"B1", "x":4, "y":1, "orientation":"h"}
```
 
Der Client teilt dem Server mit, dass der Spieler ein Schiff auf seinem Spielfeld platziert hat. Damit der Server dieses Event akzeptiert, muss der Schiffstyp (siehe **Regelset**) im Feld `shiptype`, die Koordinaten in den Feldern `x` und `y`, sowie die Ausrichtung des Schiffes im Feld `orientation` mitgesendet werden. Die beiden Koordinatenteile `x` und `y` müssen sich im Wertebereich **von 0 (inklusive) bis 9 (inklusive)** befinden. Die Ausrichtung im Feld `orientation` ist bei horizontaler Platzierung `"h"` und bei vertikaler Ausrichtung `"v"`. Die Koordinaten beschreiben hierbei die oberste bzw. linkeste Koordinate des platzierten Schiffes, die Größe des Schiffes wird durch den Schiffstyp definiert.  
 
> field_update
 
```json
{"action":"field_update", "type":"your_field", "data":{"hits":[[0,0], [1,0], [2,0]], "missed":[[0,1], [1,1], [2,1]], "ships":[{"type":"C1", "x":0, "y":0, "orientation":"h", "hits":[true, true, true, false]}], ...}}
```
 
Der Server teilt dem Client eine Aktualisierung eines Spielfeldes mit. Dabei unterscheidet sich anhand des Feldes `type`, um wessen Spielfeld es sich handelt. Mögliche Werte sind: `your_field`, für das eigene und `opponent_field` für das gegnerische Spielfeld. Die eigentlichen Spielfelddaten befinden sich im Unterobjekt `data`.  
Das Feld `hits` beinhaltet ein Array aus Koordinaten, an welchen der jeweils gegnerische Spieler auf dem jeweiligen Spielfeld bereits gefeuert und getroffen hat. Die Koordinaten selbst bestehen hierbei aus einem 2-Elementigen Array mit dem X-Wert als 1. und dem Y-Wert als 2. Element.  
Das Feld `missed` beinhaltet ein weiteres Array aus Koordinaten, an welchen der jeweils gegnerische Spieler auf dem jeweiligen Spielfeld bereits gefeuert, jedoch nichts getroffen hat. Die Koordinaten selbst bestehen hierbei ebenfalls aus einem 2-Elementigen Array mit dem X-Wert als 1. und dem Y-Wert als 2. Element.   
Das Feld `Ships` beinhaltet ein Array aus Objekten, welches die Flotte auf dem jeweiligen Spielfeld repräsentiert. Ein Schiff Objekt hat dabei die Felder `type`, welches des Schiffstype  (siehe **Regelset**) beinhaltet, 2 Koordinaten-Felder `x` und `y`, der Ausrichtung `orientation` als Kurzform `h` oder `v`, sowie ein Array aus Boolean-Werten, welches die Treffer des jeweiligen Schiffes repräsentieren. Der erste Wert entspricht dabei der obersten bzw. linkesten Koordinate des Schiffes. Der Wert `true` steht für einen Treffer, der Wert `false` für ein unbeschädigtes Schiffteil.  
** Achtung, Besonderheit**!  
Handelt es sich bei der Spielfeldaktualisierung um das eigenen Spielfeld, so werden während der Schiffe-Platzieren-Phase nur diejenigen Schiffe, die der Spieler bereits gültig auf seinem Spielfeld platziert hat, übermittelt. Des weiteren werden die Felder `x`, `y` und `orientation` bei Aktualisierungen des gegnerischen Spielfeldes erst dann übermittelt, wenn das Schiff komplett zerstört wurde. Eine Abfrage, ob die Felder vorhanden sind, wäre dementsprechen für eine Fallunterscheidung angebracht. Außerdem werden die Werte des Schiffs-Feldes `hits` solange alle auf `false` stehen, bis das gesamte Schiff zerstört wurde. Ab dem Moment der Zerstörung wird dann das Array gefüllt mit dem Wert `true` übertragen.
 
> attack
 
```json
{"action":"attack", "x":4, "y":1}
```
 
Der Client teilt dem Server mit, dass der Spieler auf ein bestimmtes gegnerisches Feld feuern möchte. Die Felder `x` und `y` stellen hierbei die Koordinaten des Angriffs dar. Sie müssen zwischen 0 (inklusive) und 9 (inklusive) liegen. Handelt es sich bei dem Angriff um einen gültigen Spielzug, wertet der Server den Zug aus und sendet Spielfeld-Aktualisierungen an beide Spieler (siehe `field_update`).
 
> gameover
 
```json
{"action":"gameover", "outcome":"winner"}
```
 
Der Server teilt dem Clienten mit, dass das Spiel abgeschlossen wurde. Im Feld `outcome` wird dabei übermittelt, um welche Art von Spielende es sich handelt. Möglich Werte sind dabei:
 *  `winner` Der Spieler hat das Spiel gewonnen
 *  `loser` Der Spieler hat das Spiel verloren
 *  `player_left` Der Gegenspieler hat das Spiel verlassen. **Zu Berücksichtigen:** Das `player_left` Outcome wird auch nach regulär gewonnenem/verlorenem Spiel gesendet, sobald der Gegenspieler die Verbindung unterbricht. Es sollte daher im Client unterschieden werden, wie auf das Outcome `player_left` reagiert wird.  
 
Das Spielende sollte dem Spieler angezeigt werden. Auch nach Spielende soll es möglich sein, mit dem Gegner zu chatten, solange bis einer der Spieler das Spiel verlässt. Es ist daher also notwendig, den Spieler in Kenntnis zu setzen, wenn der andere Spieler das Spiel verlassen hat und ggf. auch die Eingabemöglichkeit zu sperren. Der Socket soll bis zum ausdrücklichen Verlassen des Spiels durch den Spieler geöffnet bleiben.  
 
> send_chat
 
```json
{"action":"send_chat", "message":"Hello BattleShip!"}
```
 
Der Client teilt dem Server mit, dass der Spieler eine Chat-Nachricht eingegeben und abgeschickt hat. Der Server leitet diese nur an den Gegenspieler weiter, der Client muss sich also selbst darum kümmern, dass die Nachricht im eigenen Chat-Bereich korrekt angezeigt wird.
 
> chatmsg
 
```json
{"action":"chatmsg", "message":"Hello BattleShip!"}
```
 
Der Server teilt dem Client mit, dass eine neue Chat-Nachricht mit dem Inhalt `message` vom Gegenspieler eingetroffen ist. Der Client sollte die Nachricht mit dem Namen des Gegenspielers als Absender im Chat anzeigen. Eine zusätzliche visuelle Benachrichtigung über die neue Nachricht wäre denkbar.
 
> logmsg
 
```json
{"action":"logmsg", "sender":"Superman", "message":"Alle Schife platziert!"}
```
 
Der Server teilt dem Client mit, dass eine neue Log-Nachricht angezeigt werden soll. Da die Log-Nachrichten vom Server generiert werden, wird zusätzlich zur Log-Nachricht im Feld `message` auch der betreffende Spielername im Feld `sender` mitgesendet. 
 
> error
 
```json
{"action":"error", "errmsg":"So geht das nicht!"}
```
 
Der Server teilt dem Client mit, dass ein von ihm gesendetes Paket ungültig ist und nicht bearbeitet wurde. Eine mögliche Fehlermeldung zum Debuggen steht im Feld `errmsg`. Die Meldung sollte nach system.err umgeleitet werden, damit während der Entwicklung Fehler erkannt werden können.
 
 
### Anhang
##### Beispiel pom.xml zur Benutzung mit maven bzw. dem Eclipse - Plugin m2e
```
<?xml version="1.0"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
<modelVersion>4.0.0</modelVersion>
<groupId>BattleShip</groupId>
<artifactId>BattleShip</artifactId>
<version>0.0.1-SNAPSHOT</version>
<build>
    <sourceDirectory>src</sourceDirectory>
    <plugins>
        <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.1</version>
            <configuration>
                <source>1.8</source>
                <target>1.8</target>
            </configuration>
        </plugin>
    </plugins>
</build>
<dependencies>
    <dependency>
        <groupId>javax.websocket</groupId>
        <artifactId>javax.websocket-api</artifactId>
        <version>1.1</version>
    </dependency>
    <dependency>
      <groupId>org.eclipse.jetty.websocket</groupId>
      <artifactId>javax-websocket-client-impl</artifactId>
      <version>9.2.11.v20150529</version>
    </dependency>
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.3.1</version>
    </dependency>
</dependencies>
</project>```
