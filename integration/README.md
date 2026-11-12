# Testy integracyjne na Paperze

Przygotuj dwie izolowane instancje Paper 26.3 build 178 na różnych portach loopbacku. Użyj Java 25, Python 3.10 lub nowszego, WorldGuard 7.0.19, WorldEdit 7.4.6-beta-02 i FakePlayerPlugin 2.1.0. Na jednej instancji umieść oficjalny JAR ProtectionStones 2.10.6, na drugiej JAR z poprawką.

Ustaw `server-ip=127.0.0.1` oraz `level-name=qa_world`. Dla typu kamienia `EMERALD_ORE` ustaw `tp_waiting_seconds = 3` i `no_moving_when_tp_waiting = true`. W FakePlayerPlugin wyłącz `persistence.enabled`, żeby bot po restarcie nie wyprzedził fixture. Tester sam tworzy syntetycznego gracza i działkę. W czasie prób instancja nie może mieć innych graczy.

Zbuduj tester w PowerShell, przekazując katalog bibliotek uruchomionego Papera oraz JAR-y ProtectionStones i FakePlayerPlugin.

```powershell
./integration/Build-Fixture.ps1 -PaperLibraries $PaperLibraries -ProtectionStonesJar $ProtectionStonesJar -FakePlayerJar $FakePlayerJar
```

Skopiuj wygenerowany `PsHomeScenarios.jar` do obu instancji. W poniższych poleceniach `lab-oryginalny` i `lab-poprawiony` to katalogi przygotowanych instancji. Zacznij od zestawu zachowania na świeżych światach, żeby objąć `/ps home` przy jednej działce.

```sh
python integration/Run-Local.py lab-oryginalny --label behavior
python integration/Run-Local.py lab-poprawiony --label behavior
python integration/Run-Local.py lab-oryginalny --race buggy --label race
python integration/Run-Local.py lab-poprawiony --race fixed --label race
python integration/Run-Local.py lab-poprawiony --mode restart-arm --label restart-before
python integration/Run-Local.py lab-poprawiony --mode restart-check --label restart-after
```

Test wyścigu używa lokalnego debuggera wyłącznie w izolowanej instancji. `PASS race-buggy` oznacza odtworzenie błędu w oryginale. `PASS race-fixed` oznacza jeden udany teleport do drugiego celu i zero pozostałych zadań. Zestaw zachowania na oryginale może zgłosić niepowodzenia po seriach komend, jeśli wyścig wystąpi bez wymuszania. Próbę restartu można powtórzyć z oryginalnym JAR-em tymi samymi poleceniami.
