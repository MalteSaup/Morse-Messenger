
// Sendervariablen:
#define txPin 2       // Auf welchem Pin senden wir?
String input;         // String der über BT vom Handy kommt
String output;        // Zeichenkette des aktuell zu sendenden Buchstabens
int cycletime = 10;   // Länge eines Punktes in ms

// Empfängervariablen
#define rxPin A3      // Auf welchem Pin empfangen wir?
int lastRx = 0;       // Bool der letzten Flanke (HIGH=1/LOW=0)
int currRx = 0;       // Bool der aktuellen Flanke (HIGH=1/LOW=0)
int lastCharBegin = 0;  // Wann hat der letzte Charakter angefangen? In ms
int lastBreakBegin = 0; // Wann hat die letzte Pause angefangen? in ms
int charTime = 0;       // Länge des letzten Charakters in ms
unsigned int breakTime = 0; // Länge der letzten Pause in ms (Kann lang werden)
String receivedWord = {};   // Bisher empfangene Zeichenkette des aktuellen Buchstabens
int clk = 0;                // Der aktuell vermutete Sendetakt des Senders (wird laufend neu berechnet)
int currRxAnalog = 0;       // Aktueller analoger Wert des Lichtsensors
int minRx = 1000;           // niedrigster Wert den der Sensor ausgegeben hat
int maxRx = 1;              // höchster Wert den der Sensor ausgegeben hat
int divider = 0;            // Mitte von minRx und MaxRx. Alles größer ist HIGH, alles niedriger ist LOW
String Wort = {};           // Die insgesamt empfangene Nachricht. 

// Alphabet und Morsecode
//-----------------------------------------------------------------
char *letterCode[] = {"-.-.-", ".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", ".---", "-.-", ".-..", "--", "-.", "---", ".--.", "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--..", ".-.-.", ".----", "..---", "...--", "....-", ".....", "-....", "--...", "---..", "----.", "-----", ".-.-.-", "--..--", "..--.."};
char letter[] = { ' ', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '>', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '.', ',', '?'};
//-----------------------------------------------------------------


void setup() {
  Serial.begin(9600);
  Serial.println("begin");
  Serial.setTimeout(3);
  pinMode(txPin, OUTPUT);
  pinMode(4, INPUT_PULLUP);
}


void loop() {
  // Wir ermitteln die Mitte zwischen höchstem und niedrigstem Punkt des analogen Signals (divider) und entscheiden daran ob es HIGH oder LOW ist
  //----------------------------------------------------------
  currRxAnalog = analogRead(rxPin);

  if (currRxAnalog < minRx) {        // falls minPeak: Wert aktualisieren, divider aktualisieren
    minRx = currRxAnalog;
    divider = (maxRx + minRx) / 2;
  }
  if (currRxAnalog > maxRx) {        // falls maxPeak: Wert aktualisieren, divider aktualisieren
    maxRx = currRxAnalog;
    divider = (maxRx + minRx) / 2;
  }
  if (currRxAnalog > divider + 5 && lastRx == LOW) currRx = HIGH; // divider +5 / -5 um das Grundrauschen des Sensors aufzufangen
  if (currRxAnalog < divider - 5 && lastRx == HIGH) currRx = LOW;
  //-----------------------------------------------------------

  if (lastRx != currRx) {                       // Wenn sich was im Signal verändert hat, starte interpret()
    interpret();
    lastRx = currRx;
  }

  input = Serial.readStringUntil("/n");         // Falls nicht landen wir hier und gucken ob wir gerade vom Smartphone empfangen
  if (input != NULL) wordSend();                // Falls wir irgendwas empfangen haben, schicke es mit wordSend()

}


void interpret() {                             // Interpret wird aufgerufen wenn sich was im Signal verändert hat (Wechsel von HIGH/LOW) und interpretiert was das war
  if (lastRx < currRx) {                       // Falls zutreffend sind wir am Zeichenbeginn/Pausenende
    breakTime = millis() - lastBreakBegin;     // BreakTime = Wie lange war die Pause?
    lastCharBegin = millis();

    if (breakTime > 7 * clk) {                 // Wenn die Pause länger als 7 Takte war -> Wortende. Alles neu. (Provisorisch)
      receivedWord = "";
      Wort = "";
    }

    if ((breakTime > 2 * clk)) {               // Check auf Buchstabenende, drei Zeichen Pause. Übersetzung
      translateMessage();
    }
  }

  if (lastRx > currRx) {                       // Falls zutreffend sind wir am Zeichenende/Pausenanfang
    charTime = millis() - lastCharBegin;
    lastBreakBegin = millis();

    if (receivedWord.length() > 6) {           // Failsave. Buchstaben sind max. 6 Zeichen lang. Falls was längeres auftaucht haben wir uns vertan und müssen resetten.
      Serial.print("_");
      receivedWord = "";
      minRx = 1000;
      maxRx = 1;
    }

    if (charTime >= 2 * clk) {
      clk = charTime / 3;
      receivedWord += '-';
    }

    if (charTime < 2 * clk) {
      clk = charTime;
      receivedWord += '.';
    }
  }
}

void translateMessage() {                     // Übersetzt das empfangene Wort, checkt vorher ob der Kontrollcharakter für "Ende" empfangen wurde

  if (receivedWord == ".-.-.") {
    Serial.println();
    Serial.println("Message received");
    return;
  }

  for (int j = 0; j < (sizeof(letterCode) / sizeof(letterCode[0])); j++) {
    if (receivedWord == letterCode[j]) {
      Wort += letter[j];
      Serial.print(letter[j]);
      receivedWord = "";
    }
  }
}

void wordSend() {
  input.toLowerCase();                          // Morse kann nur Kleinbuchstaben
  input = ' ' + input;                          // An den Anfang kommt ein Leerzeichen als Kalibrierung
  input[input.length() - 1] = '>';              // Ans Ende kommt ein > als  Endcharakter (wird extra abgefangen)
  Serial.println(input);

  for (int i = 0; i < input.length(); i++) {
    charSend(input.charAt(i));
  }
}

void charSend(char toSend) {                    // Ich bekomme den zu sendenden Buchstaben
  for (int j = 0; j < sizeof(letter); j++) {    // Buchstaben in Array suchen, entsprechende Zeichenfolge in Output schreiben
    if (toSend == letter[j]) {
      output = letterCode[j];
    }
  }
  
  for (int k = 0; k <= output.length(); k++) {  // Sendecode
    zPause();
    if (output.charAt(k) == '.') punkt();
    if (output.charAt(k) == '-') strich();
  }
  Serial.print(toSend);
  bPause();
}

void punkt() {
  digitalWrite(txPin, HIGH);
  Serial.print(".");
  delay(cycletime);
  digitalWrite(txPin, LOW);
}

void strich() {
  digitalWrite(txPin, HIGH);
  Serial.print("-");
  delay(cycletime * 3);
  digitalWrite(txPin, LOW);
}


void zPause() {   // Pause zwischen Zeichen
  delay(cycletime);
}

void bPause() {   // Pause zwischen Buchstaben
  Serial.println("/");
  delay(cycletime * 1.5);
}
