# Instalacja i wycofanie poprawki

Poprawka jest przeznaczona dla ProtectionStones 2.10.6. Wydanie testowano z Paper 26.3 build 178, Java 25, WorldGuard 7.0.19 i WorldEdit 7.4.6-beta-02.

1. Przygotuj oddzielny serwer testowy i syntetyczną działkę. Przed zmianą zachowaj kopię oryginalnego JAR-a oraz konfiguracji.
2. Porównaj SHA-256 pobranego JAR-a z plikiem sum kontrolnych z wydania. Na Windows możesz użyć `Get-FileHash` z algorytmem SHA-256.
3. Zatrzymaj instancję testową. Wymień JAR ProtectionStones i sprawdź, czy w katalogu pluginów pozostała jedna kopia.
4. Uruchom test. Ustaw trzy sekundy oczekiwania z włączoną kontrolą ruchu. Zwykłym kontem bez bypassu sprawdź bezruch, obrót kamery, ruch w trakcie odliczania, dwie szybkie próby do różnych działek i wylogowanie podczas odliczania.
5. Do wycofania zatrzymaj test, przywróć oryginalny JAR i ponownie uruchom instancję. Poprawka nie wymaga migracji działek.

[Wyniki wydania](RAPORT-WYDANIA.md) · [Automatyczna regresja](integration/README.md)
