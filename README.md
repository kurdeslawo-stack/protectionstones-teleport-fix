# Poprawka timerów teleportacji w ProtectionStones 2.10.6

Dwie nakładające się próby `/ps home` mogły sprawić, że starszy timer anulował nowszą próbę tego samego gracza. Patch przenosi rejestrację i anulowanie na główny wątek oraz przypisuje sprzątanie do konkretnego zadania. Zachowuje czas odliczania, kontrolę ruchu i uprawnienie `protectionstones.tp.bypasswait`.

## Nałożenie poprawki

1. Użyj źródeł z [oficjalnego tagu 2.10.6](https://github.com/espidev/ProtectionStones/releases/tag/2.10.6), commit `7701764b5cda7db4ef59d1c99252ac7cc19c98f8`.
2. Umieść plik patcha w katalogu głównym tego checkoutu.
3. Sprawdź i nałóż poprawkę.

```sh
git apply --check protectionstones-2.10.6-teleport-timer.patch
git apply protectionstones-2.10.6-teleport-timer.patch
```

## Sprawdzenie na Paperze

1. Na oddzielnym serwerze testowym zachowaj kopię oryginalnego JAR-a. Zatrzymaj serwer, podmień JAR i uruchom serwer z jedną kopią ProtectionStones.
2. Ustaw trzy sekundy oczekiwania dla testowego typu działki i włącz kontrolę ruchu. Użyj zwykłego konta bez uprawnienia omijającego odliczanie.
3. Sprawdź bezruch, obrót kamery, ponowienie komendy, ruch w trakcie odliczania i wylogowanie. W regresji wymuszono nałożenie dwóch prób do różnych celów. Po przesunięciu między próbami gracz stał nieruchomo, a poprawiony JAR teleportował go raz, do drugiego celu.
4. Cofnięcie polega na przywróceniu oryginalnego JAR-a przy zatrzymanym serwerze testowym.

[Raport zawiera wyniki 27 scenariuszy, porównanie JAR-ów i status pełnej kompilacji Maven.](RAPORT-TESTOW.md)
