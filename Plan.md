# 0. Предпосылки — что уже должно быть готово

* Все `Message`, `MessageRequest`, `MessageResponse`, `MessageResult` и т. п. реализованы и `implements Serializable`.
* Game-логика (`GameSession`, `Player`, `Ship` и т. д.) работает и у тебя уже в проекте.
* Пакетная структура организована (например `seaBattle.protocol.messages`, `seaBattle.server`, `seaBattle.client`, `seaBattle.gameLogic`). Очень важно, чтобы **одна и та же версия классов `Message` была и на клиенте, и на сервере** (совместимость классов/serialVersionUID).

---

# 1. Структура проекта (рекомендуемая)

```
seaBattle/
 ├─ common/
 │   └─ src/
 │      └─ seaBattle/protocol/...   (Message, Protocol, Message* классы)
 ├─ server/
 │   └─ src/
 │      └─ seaBattle/server/...     (ServerMain, SeaBattleServiceImpl, Server logic)
 └─ client/
    └─ src/
       └─ seaBattle/client/...      (ClientMain, пользовательский UI)
```

**Идея:** `common` — общий код, компилируется и включается в classpath и клиента, и сервера.

---

# 2. Что нужно добавить (общее понимание)

* RMI-интерфейс (Remote) `SeaBattleService` — методы принимают и возвращают `Message*` объекты.
* Серверная реализация `SeaBattleServiceImpl` extends `UnicastRemoteObject` — внутри вызывает существующую логику (ранее — в `ServerClientHandler`).
* Клиент вызывает методы RMI вместо `ObjectOutputStream.writeObject(...)`/`readObject()`.
* (Опционально) RMI-callback интерфейс `ClientCallback extends Remote`, чтобы сервер мог пушить ассинхронные уведомления (челленджи, ходы противника и т. п.) на клиента.

---

# 3. Проектирование RMI-интерфейса (методы)

Создай интерфейс `seaBattle.rmi.SeaBattleService extends Remote`. Пример набор методов (всё возвращает/принимает `Message*`):

* `MessageResult connect(MessageConnect req) throws RemoteException;`
* `MessageResult disconnect(MessageDisconnect req) throws RemoteException;`
* `MessageResult userList(MessageUser req) throws RemoteException;`
* `MessageChallengeResult createChallenge(MessageChallenge req) throws RemoteException;`
* `MessageChallengeResult answerChallenge(MessageChallengeResponse resp) throws RemoteException;`
* `MessagePlaceShipsResult placeShips(MessagePlaceShips req) throws RemoteException;`
* `MessageResult readyToPlay(MessageReadyToPlay req) throws RemoteException;`
* `MessageMoveResult move(MessageMove req) throws RemoteException;`
* `MessageGetFieldResult getField(MessageGetField req) throws RemoteException;`
* `MessageGameOver forfeit(MessageForfeit req) throws RemoteException;`

> Примечание: подбирай реальные типы `Message*`, которые у тебя есть (например `MessageMoveResult` и т.д.).

---

# 4. Общая логика серверной реализации

* `SeaBattleServiceImpl extends UnicastRemoteObject implements SeaBattleService`.
* Внутри используй тот же `ServerMain`/`GameSession`/`Challenge` хранилища, только обращение к ним будет в методах интерфейса (а не в `switch(msg.getID())`).
* Методы должны быть **синхронизированы** где надо (локи/ConcurrentHashMap) — у тебя уже была синхронизация в `ServerMain`, перепользуй её.
* Каждый метод возвращает соответствующий `MessageResult`/`Message*` объект с кодом результата и/или данными.

Пример рабочего подхода:

* `connect(req)`: проверяет `registerUser(req.getNic(), this?)` — но тут нельзя регистрировать `this` (RMI объект) как клиент-handler. Вместо этого:

  * При `connect` сервер сохраняет `nic->ClientCallback` (если используешь callback) или просто помнит nic и метаданные (если без callback).
  * Возвращает `MessageConnectResult`.
* `createChallenge(req)`: как раньше — создаёт `Challenge` объект с id, сохраняет в map, и — если используешь callback — вызывает `callback.onChallengeRequest(...)` у адресата; если нет callback — возвращает результат и клиент-инициатор должен ждать/опросить.
* `answerChallenge(resp)`: ищет challenge по id, при согласии создаёт `GameSession` (id session), возвращает `MessageChallengeResult` (с id), и уведомляет обоих игроков (через callback или клиент опрашивает).
* `move(req)`: вызывает `GameSession.move(...)`, возвращает `MessageMoveResult` с полем `enemyField` (как у тебя), и — если есть callback — уведомляет оппонента.

---

# 5. Архитектурное решение: push уведомления (callback) — рекомендую

RMI удобнее с callback — сервер вызывает методы на клиенте для уведомлений (например: пришёл челлендж, пришёл ход, матч стартовал).

* Создай интерфейс `ClientCallback extends Remote`:

  * `void onChallengeRequest(MessageChallengeRequest req) throws RemoteException;`
  * `void onGameStart(MessageGameStart start) throws RemoteException;`
  * `void onMoveResult(MessageMoveResult move) throws RemoteException;`
  * `void onGameOver(MessageGameOver over) throws RemoteException;`
  * `...`

* Клиент при `connect(...)` передаёт в параметре объект `ClientCallback` (реализацию, экспортированную через `UnicastRemoteObject`) — сервер сохраняет эту ссылку для ников.

* Сервер вызывает методы callback у нужного клиента (пример: `callback.onChallengeRequest(...)`).

**Если не использовать callback**, придётся реализовать опрос (poll) с периодическими вызовами `service.getUpdates(nic)` — менее удобно.

---

# 6. Синхронизация и Thread-safety

* RMI сам создаёт вызовы в отдельных потоках, поэтому:

  * Синхронизируй доступ к `gameSessions`, `users`, `challenges` (как ты уже делал с ConcurrentHashMap + синхронизированными блоками).
  * За элементы с внутренним состоянием (`GameSession`) — пометь ключевые методы как `synchronized` (у тебя уже так).
* Проверяй граничные случаи: игра не началась, не твой ход — кидай `MessageError`/`MessageResult` с кодом ошибки.

---

# 7. Обработка идентификаторов (challengeId и sessionId)

* В RMI-методах обязательно возвращай/передавай `challengeId` и `sessionId` так же, как в TCP-версии.
* Т. к. теперь вызов синхронный, инициатор сразу получает `MessageChallengeResult` (с id), но челлендж-реквест нужно доставить адресату через callback или опрос.

---

# 8. Файлы policy / безопасность (локально обычно не нужно)

Для локальной демонстрации можно запускать без специального security manager. Если понадобятся callback и export object, для простоты не ставь `SecurityManager`. Если всё же требуется — добавь файл `policy` с `grant { permission java.security.AllPermission; };` при тестировании.

---

# 9. Сборка и запуск без Maven (javac / java)

Рекомендую компиляцию в 2 шага: сначала `common`, затем `server` и `client`, включая `common` в classpath.

Пример команд (bash, из корня проекта `seaBattle`):

```bash
# 1) компиляция common
mkdir -p build/common
javac -d build/common $(find common/src -name "*.java")

# 2) компиляция server (использует common)
mkdir -p build/server
javac -d build/server -cp build/common $(find server/src -name "*.java")

# 3) компиляция client (использует common)
mkdir -p build/client
javac -d build/client -cp build/common $(find client/src -name "*.java")
```

Запуск RMI-реестра и сервера/клиента:

```bash
# Запустить rmiregistry (исполняется в background). Укажи classpath, чтобы реестр видел общие классы:
cd build/common
rmiregistry 1099 &   # или: (nohup rmiregistry 1099 &)
cd ../../

# Запустить сервер (в classpath должны быть common + server)
java -cp build/common:build/server seaBattle.server.ServerMain

# Запустить клиента (каждый в отдельном терминале)
java -cp build/common:build/client seaBattle.client.ClientMain <nic> "<fullname>" [host]
```

> Если rmiregistry недоступен, можно программно реестровать:
>
> ```java
> LocateRegistry.createRegistry(1099);
> ```

---

# 10. Конкретные шаги реализации (пошагово)

### Шаг A — Подготовка общего кода

1. Проверь, что все `Message*` классы находятся в `common/src/seaBattle/protocol/...` и `implements Serializable`. Присвой явный `serialVersionUID`.
2. Убедись, что пакеты одинаковые в client и server (использовать общий `common`).

### Шаг B — Создать RMI интерфейс

3. В `common` добавь `seaBattle.rmi.SeaBattleService` с методами из раздела 3.
4. Добавь интерфейс `seaBattle.rmi.ClientCallback` (если хочешь push).

### Шаг C — Серверная реализация

5. В `server` добавь `SeaBattleServiceImpl extends UnicastRemoteObject implements SeaBattleService`.

6. В `ServerMain`:

   * Создавай/регистрируй `SeaBattleServiceImpl` в RMI Registry:

     ```java
     LocateRegistry.createRegistry(1099);
     Naming.rebind("SeaBattleService", serviceImpl);
     ```
   * Либо `Registry registry = LocateRegistry.getRegistry(); registry.rebind("SeaBattleService", serviceImpl);`

7. Перенеси логику из `ServerClientHandler` в методы `SeaBattleServiceImpl`.

   * `connect(...)` — регистрирует ник -> callback (если есть) или помечает пользователя подключённым.
   * `createChallenge(...)` — создаёт challenge, сохраняет его, уведомляет адресата через callback.
   * `answerChallenge(...)` — создаёт GameSession и уведомляет обоих.
   * `move(...)` — вызывает `GameSession.move(...)` и возвращает `MessageMoveResult`, дополнительно уведомляя оппонента через callback.

8. Для логирования добавь `ServerMain.log()` (уже делал) и используешь в методах.

### Шаг D — Клиент

9. В `client`:

   * Реализуй `ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback` — в нём вывод в консоль и обновление локального состояния (pendingChallenges, currentSessionId и т.д.).
   * При старте клиента:

     ```java
     Registry reg = LocateRegistry.getRegistry(host);
     SeaBattleService service = (SeaBattleService) reg.lookup("SeaBattleService");
     ClientCallback cb = new ClientCallbackImpl(...);
     MessageConnect req = new MessageConnect(nic, name, cb?); // либо отдельный метод connectWithCallback
     MessageConnectResult res = service.connect(req);
     ```
   * Дальше клиент вызывает `service.createChallenge(...)`, `service.move(...)` и т.д. Получает `Message*` как результат.

10. В клиенте замени все `ObjectInputStream/ObjectOutputStream` вызовы на прямые вызовы сервис-методов.

### Шаг E — Тестирование

11. Запусти `rmiregistry`.
12. Запусти `ServerMain`.
13. Запусти два клиента в отдельных терминалах, подключись, создай челлендж, ответь — проверь, что callback-уведомления приходят.
14. Проверь сценарии: placeShips, ready, moves (очередность), forfeit, отключение клиента.

---

# 11. Отдельные важные моменты и советы

* **Версия классов:** при изменениях `Message` меняй `serialVersionUID` и обновляй обе стороны. Лучше фиксировать и не менять.
* **Callback и firewall:** при callback JVM клиенты экспортируют удалённые объекты; если клиенты находятся за NAT/файерволом — могут быть проблемы. Для локальной демонстрации всё OK.
* **Timeout / отказоустойчивость:** методы могут бросать `RemoteException` — клиент должен обработать (повтор/сообщить).
* **Логи:** добавь ясные логи на сервере (connect/disconnect/challenge/session start/end/move).
* **Безопасность:** для демонстрации можно дать `AllPermission` в локальном policy; в реальном проекте нужно ограничивать.

---

# 12. Примерный мэппинг TCP -> RMI

* TCP `MessageChallenge` отправлялся в сокет — в RMI ты вызываешь `service.createChallenge(msg)`.
* Ответы, которые раньше приходили через ObjectStream, теперь возвращаются как результат метода или через callback `client.onChallengeRequest(...)`.

---

# 13. Пример сценария запуска (итоговые команды)

1. Компаил общих классов:

```bash
javac -d build/common $(find common/src -name "*.java")
```

2. Компилируй сервер/клиент:

```bash
javac -d build/server -cp build/common $(find server/src -name "*.java")
javac -d build/client -cp build/common $(find client/src -name "*.java")
```

3. Запусти rmiregistry:

```bash
(cd build/common && rmiregistry 1099 &)   # запустит rmiregistry в фоновом режиме
```

4. Запусти сервер:

```bash
java -cp build/common:build/server seaBattle.server.ServerMain
```

5. Запусти клиентов (в отдельных терминалах):

```bash
java -cp build/common:build/client seaBattle.client.ClientMain nickA "Alice" localhost
java -cp build/common:build/client seaBattle.client.ClientMain nickB "Bob" localhost
```

---

# 14. Тестовые сценарии (что проверить)

* Подключение/отключение клиентов.
* Список пользователей (`userList`).
* Создание челленджа и доставку уведомления адресату.
* Отказ/принятие челленджа (создание `GameSession`).
* Размещение кораблей (рандом/ручной) и проверка `placeShips`.
* Готовность (`readyToPlay`) — старт игры.
* Ход (`move`) — проверка очередности, `MessageMoveResult` содержит `enemyField`.
* Forfeit — завершение игры.
* Непредвиденные: клиент неожиданно закрывается — сервер должен удалить пользователя и отменить связанные вызовы/challenges/sessions.

---

# 15. Варианты расширения (после базовой реализации)

* GUI вместо консоли (JavaFX/Swing).
* Логи в файл.
* Аутентификация/пароли.
* Сохранение статистики/результатов.

---

Если хочешь, могу в следующем сообщении:

1. Написать **текст интерфейса `SeaBattleService`** и **`ClientCallback`** (у тебя уже есть Message классы).
2. Показать **простейшую реализацию `SeaBattleServiceImpl`** с методом `connect` и `createChallenge` (микро-пример).
3. Подготовить **скрипты сборки/запуска** (bash) для твоей текущей структуры файлов.

Напиши, что предпочитаешь — сразу интерфейсы + пример impl, или сначала скрипты сборки и запуск?
