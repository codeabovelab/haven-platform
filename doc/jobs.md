# Jobs #

System has internal job engine. Engine provide list of job types and job instances.
Job type has description of job with list of all its parameters and types. A job instance - handle for running task 
of specified job type. 

Instance can be created from `JobParameters` object. Note that engine identity job by its 
parameters, so if you pass parameters twice - system does not run job again.
Job instances is persist in memory for execution time, and remain there for configured time (default on day). 
After then it removed (by timeout or manually), you can run it again with same parameters. When you need to run job 
twice you must add any additional parameter (for example `'counter': i` where i will incremented for each job).  
You can retrieve status of each executed job, its logs, or read runtime log stream.

### Scheduling ###

Any job can be scheduled, it done by adding 'schedule' parameter. 

Look, below a simple job:

```js
{
  "type": "job.sample",
  "parameters":{
      "inParam":"test"
  }
}
```

And now scheduled job:

```js
{
  "type": "job.sample",
  "schedule" : "*/10 * * * * *", // every ten seconds.
  "parameters":{
      "inParam":"test"
  }
}
```

Which is executed every 10 seconds. You can rollback it too, but rollback only last iteration. Also note that this 
jobs does not stop on their own, but you can stop and remove its.  

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

