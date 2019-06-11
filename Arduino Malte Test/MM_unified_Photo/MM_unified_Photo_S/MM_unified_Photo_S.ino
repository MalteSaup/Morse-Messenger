
/*
#include <SoftwareSerial.h>
SoftwareSerial BTSerial(10,11);
*/

#define BTSerial Serial1


// Sendervariablen:
#define txPin 2       // Auf welchem Pin senden wir?
String input;         // String der über BT vom Handy kommt
String output;        // Zeichenkette des aktuell zu sendenden Buchstabens
int cycletime = 20;   // Länge eines Punktes in ms

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
int lastRxAnalog = 0;
int minRx = 1000;           // niedrigster Wert den der Sensor ausgegeben hat
int maxRx = 1;              // höchster Wert den der Sensor ausgegeben hat
int divider = 0;            // Mitte von minRx und MaxRx. Alles größer ist HIGH, alles niedriger ist LOW
String Wort = {};           // Die insgesamt empfangene Nachricht. 
String cycleSet;
int peakResetTime = 0;

// Alphabet und Morsecode
//-----------------------------------------------------------------
char *letterCode[] = {"-.-.-", ".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", ".---", "-.-", ".-..", "--", "-.", "---", ".--.", "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--..", ".-.-.", ".----", "..---", "...--", "....-", ".....", "-....", "--...", "---..", "----.", "-----", ".-.-.-", "--..--", "..--..","-....-"};
char letter[] = { ' ', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '>', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '.', ',','?',':'};
//-----------------------------------------------------------------


void setup() {
  Serial.begin(9600);
  Serial.println("begin");
  BTSerial.setTimeout(3);
  Serial.setTimeout(1);
  pinMode(txPin, OUTPUT);
  pinMode(4, INPUT_PULLUP);
  BTSerial.begin(9600);

}


void loop() {

  // Wir ermitteln die Mitte zwischen höchstem und niedrigstem Punkt des analogen Signals (divider) und entscheiden daran ob es HIGH oder LOW ist
  //----------------------------------------------------------
  lastRxAnalog = currRxAnalog;
  currRxAnalog = analogRead(rxPin);
  minRx++;
  maxRx--;

  // Serial.println(currRxAnalog);
  // return;

  if (currRxAnalog < minRx) {        // falls minPeak: Wert aktualisieren, divider aktualisieren
    minRx = currRxAnalog;
    divider = (maxRx + minRx) / 2;
  }
  if (currRxAnalog > maxRx) {        // falls maxPeak: Wert aktualisieren, divider aktualisieren
    maxRx = currRxAnalog;
    divider = (maxRx + minRx) / 2;
  }
  
  if ((currRxAnalog + lastRxAnalog) / 2 > divider + 4 && lastRx == LOW) currRx = HIGH; // divider +2 / -2 um das Grundrauschen des Sensors aufzufangen
  if ((currRxAnalog + lastRxAnalog) / 2 < divider - 4 && lastRx == HIGH) currRx = LOW;
  //-----------------------------------------------------------

  if (lastRx != currRx) {                       // Wenn sich was im Signal verändert hat, starte interpret()
    interpret();
    lastRx = currRx;
  }

  

  input = BTSerial.readStringUntil("/n");         // Falls nicht landen wir hier und gucken ob wir gerade vom Smartphone empfangen
  if(input != NULL){
  Serial.println(input);
  }


// Abfang von Kontrollcharakteren
//--------------------------------------------------- 
  if(input.startsWith("CLK:")){
    cycleSet = input.substring(4);
    cycletime = cycleSet.toInt();
    Serial.print("New Cycletime: ");
    Serial.println(cycletime);
    return;    
  }

  if(input.startsWith("OPEN")){
    Serial.print("OPENING");
    digitalWrite(2,HIGH);
    delay(10000);
    digitalWrite(2,LOW);
    return;    
  }
//---------------------------------------------------
  
  if (input != NULL) wordSend();                // Falls wir irgendwas empfangen haben, schicke es mit wordSend()
  String s = Serial.readStringUntil("\n");
  BTSerial.println(s  + "\r");
}




void interpret() {                             // Interpret wird aufgerufen wenn sich was im Signal verändert hat (Wechsel von HIGH/LOW) und interpretiert was das war
  if (lastRx < currRx) {                       // Falls zutreffend sind wir am Zeichenbeginn/Pausenende
    breakTime = millis() - lastBreakBegin;     // BreakTime = Wie lange war die Pause?
    lastCharBegin = millis();

    if (breakTime > 15 * clk) {                 // Wenn die Pause länger als 7 Takte war -> Wortende. Alles neu. (Provisorisch)
      receivedWord = "";
      Wort = "";
      clk = 0;
      Serial.println("NEW MESSAGE:"); 
    }

    if ((breakTime > 2 * clk)) {               // Check auf Buchstabenende, drei Zeichen Pause. Übersetzung
      translateMessage();
    }
  }

  if (lastRx > currRx) {                       // Falls zutreffend sind wir am Zeichenende/Pausenanfang und können das Zeichen interpretieren.
      charTime = millis() - lastCharBegin;
      lastBreakBegin = millis();

    if (receivedWord.length() > 6) {           // Failsafe. Buchstaben sind max. 6 Zeichen lang. Falls was längeres auftaucht gibt es Probleme und wir und müssen resetten.
      Serial.print("_");
      receivedWord = "";
      minRx = 1000;
      maxRx = 1;
    }

    if (charTime >= 2 * clk) {
      clk = charTime / 3;
      receivedWord += '-';
      Serial.print("-");
    }

    if (charTime < 2 * clk) {
      clk = charTime;
      receivedWord += '.';
      Serial.print(".");
    }
  }
}

void translateMessage() {                     // Übersetzt das empfangene Wort, checkt vorher ob der Kontrollcharakter für "Ende" empfangen wurde

  if (receivedWord == ".-.-.") {   
    receivedWord == "";
    received();
    return;
  }
  
   if (receivedWord == "-....-") {
   receivedWord == "";
   acknowledge();
   return;
  }
  
  for (int j = 0; j < (sizeof(letterCode) / sizeof(letterCode[0])); j++) {
    if (receivedWord == letterCode[j]) {
      Wort += letter[j];
      Serial.println(letter[j]);
      receivedWord = "";
    }
  }
}


void received(){

    Serial.println();
    Serial.println("Message received: ");
    BTSerial.println(Wort);  
    BTSerial.flush();
    Serial.println(Wort);
    Wort = "";  

    
    charSend(':');    //Acknowledge

}

void acknowledge(){
    Serial.println(" ");
    Serial.println("Receiver acknowledged");
}

void wordSend() {
  input.toLowerCase();                          // Morse kann nur Kleinbuchstaben
  input = ' ' + input;                          // An den Anfang kommt ein Leerzeichen als Kalibrierung
  input = input.substring(0,input.length()-1);
  input[input.length() - 1] = '>';              // Ans Ende kommt ein > als  Endcharakter (wird extra abgefangen)
  Serial.print("Gesendet wird: ");
  Serial.println(input);

  for (int i = 0; i < input.length(); i++) {
    charSend(input.charAt(i));
  }
  punkt(); 
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
  // Serial.print(".");
  delay(cycletime);
  digitalWrite(txPin, LOW);
}

void strich() {
  digitalWrite(txPin, HIGH);
  // Serial.print("-");
  delay(cycletime * 3);
  digitalWrite(txPin, LOW);
}


void zPause() {   // Pause zwischen Zeichen
  delay(cycletime);
}

void bPause() {   // Pause zwischen Buchstaben
  // Serial.println("/");
  delay(cycletime * 1.5);
}
