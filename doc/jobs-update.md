# Update jobs #

The system provides different job types which do cluster-wide batch containers update. There are different strategies of updating containers:

* `ui.updateContainers.startThenStopEach` - start new container with different name, then stop and remove old
* `ui.updateContainers.stopThenStartAll` - stop and remove all containers, then create new containers with same names
* `ui.updateContainers.stopThenStartEach` - stop and remove old container, then start new, repeat for each other

All jobs has a common list of parameters:


* cluster - name of processed cluster
    * type: string
    * required: true
* LoadContainersOfImage.percentage - 0-100 - float value for partially update  
    * type: float
    * required: false
* images - object with list of images which need update
    * type: object
    * required: true
    
    ```js
    {
        images:[
            {
                /* Name or pattern (like 'registry/image*') of image with registry, but without tag
                 '*' match all images
                 */
                name:"ya.ru/search-engine*",
                /* Comma delimited list of tag patterns from which image will be upgraded. 
                   If null then system update all versions of this image.
                 */
                from:"6.13, 7.3*",
                /* Destination tag to which image will be upgraded. If you leave 
                   null value then system will upgrade to last version. Also you can 
                   specify pattern of tag, like '*-stable'
                 */
                to:"7.40"
            }
        ],
        /* below planned for future */
        exclude: {
            nodes: ["do-not-touch-me.ya.ru"],
            containers: ["important-container"]
        }
    }
    ```

An example request:

```js
{
    type: "ui.updateContainers.stopThenStartEach",
    title: "Update test nginx & java",
    parameters: {
        cluster: "clusterName",
        "LoadContainersOfImage.percentage": 100,
        images: {
            images: [
                {
                    name: "our-repo:8080/nginx",
                    from: "*",
                    to: "latest"
                },
                {
                    name: "our-repo:8080/java",
                    from: "*",
                    to: "latest"
                }
            ]
        }
    }
}
```

Also see [tests](https://github.com/codeabovelab/haven-api-tests/blob/master/test_jobs_update.js)
