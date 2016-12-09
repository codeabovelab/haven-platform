# Jobs #

## Api ##

List job types:

```js
// GET /ui/api/jobtypes/

// result:
[
    "job.deploySource",
    "job.removeImageJob",
    "job.removeClusterImages",
    "job.sample",
    "ui.updateContainers.startThenStopEach",
    "ui.updateContainers.stopThenStartAll",
    "ui.updateContainers.stopThenStartEach",
    "job.updateToTagOrCreate",
    "job.updateToTag",
    "job.rollback"
]
```

Get job type parameters:

```js
// GET /ui/api/jobtypes/job.sample/

// result:
{
  "type": "job.sample",
  "parameters": {
    "inParam": {
      "name": "inParam",
      "type": "java.lang.String",
      "required": false,
      "in": true,
      "out": false
    },
    "outParam": {
      "name": "outParam",
      "type": "java.lang.String",
      "required": false,
      "in": false,
      "out": true
    }
  }
}
```

Run job instance:

```js 
// POST /ui/api/jobs/ 
{
  "type": "job.sample",
  "title": "A sample job title",
  /* String cron-like expression. */
  "schedule" : "*/10 * * * * *", // every ten seconds.
  "parameters":{
      "inParam":"test"
  }
}
// result:
{
  "id": "job.sample-0",
  "title": "job.sample",
  "status": "STARTED",
  "createTime": "2016-12-02T18:51:18.589",
  "startTime": "2016-12-02T18:51:18.822",
  "endTime": "+999999999-12-31T23:59:59.999999999",
  "running": true,
  "canRollback": false,
  "parameters": null
}
```

List job instances:

```js
// GET /ui/api/jobs/
// result:
[
  {
    "id": "job.sample-0",
    "title": "job.sample",
    "status": "COMPLETED",
    "createTime": "2016-12-02T18:51:18.589",
    "startTime": "2016-12-02T18:51:18.823",
    "endTime": "2016-12-02T18:51:18.853",
    "running": false,
    "canRollback": true,
    "parameters": null
  }
]
```

Rollback job instance:

```js
// POST /ui/api/jobs/ 
{
  "type": "job.rollback",
  "title": "Rollback job.sample-0",
  "parameters":{
      "jobId":"job.sample-0"
  }
}
// result:
{
  "id": "job.rollback-0",
  "title": "Rollback job.sample-0",
  "status": "STARTED",
  "createTime": "2016-12-02T18:51:18.589",
  "startTime": "2016-12-02T18:51:18.822",
  "endTime": "+999999999-12-31T23:59:59.999999999",
  "running": true,
  "canRollback": false,
  "parameters": null
}
```

