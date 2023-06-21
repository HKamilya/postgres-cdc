PostgreSQL-CDC

Для создания нового слушателя можно отправить POST-запрос на /api/connector
c телом 
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "dataType": "bytes",
  "host": "string",
  "port": "string",
  "database": "string",
  "user": "string",
  "password": "string",
  "fromBegin": true,
  "forAllTables": true,
  "tables": "string",
  "slotName": "string",
  "publicationName": "string",
  "topicName": "string",
  "saveChanges": true
}
```

Для редактирования необходимо отправить POST-запрос на /api/connector/{connectorId}
с телом 
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "host": "string",
  "port": "string",
  "database": "string",
  "user": "string",
  "password": "string",
  "topicName": "string",
  "saveChanges": true
}
```

Swagger c более подробным описанием методов доступен по адресу /swagger-ui/index.html#

Для управления коннекторами так же можно воспользоваться сервисом postgres-cdc-frontend

* Список коннекторов
![Список коннекторов](/img/main.png)

* Создание коннектора
![Создание коннектора](/img/create.png)

* Изменение коннектора
![Изменение коннектора](/img/update.png)

* Список изменений
![Список изменений](/img/update.png)