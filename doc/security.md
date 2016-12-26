## Concept ##

Haven uses Access Control List (ACL) and it is extended to use roles (GrantedAuthorities in spring terms) as the security model. It is based on [spring ACL](http://docs.spring.io/spring-security/site/docs/current/reference/html/domain-acls.html).

Each user is presented as `ExtendedUserDetails` and has a list of roles. Any authenticated user has 'ROLE_USER' too.

A user is stored in config file or KV-storage. Each case managed by different services: `ConfigurableUserDetailService` or 
`UsersStorage`. System combines it through `CompositeUserDetailsService`. 

You can see 'tenant' field in different security objects - it needed in multitenancy environment, support of this is 
planning for future releases, at now you must fill those fields with 'root' - value (`MultiTenancySupport.ROOT_TENANT`).

### Configuration ###

Config sample:

    dm.auth.adminPassword=$2a$08$bFLBfYL8Eb6n71D/yvLyLu9QzxDWEPG0TTx3/LgfiwaKdhfyCEdVe
    dm.auth.user[second].password=$2a$08$bFLBfYL8Eb6n71D/yvLyLu9QzxDWEPG0TTx3/LgfiwaKdhfyCEdVe
    dm.auth.user[second].email=se@co.nd
    dm.auth.user[second].title=Mr. Second
    dm.auth.user[second].roles=DEVELOPER, GC@java 

The password uses bcrypt hash and can created by 
`read pwd && python -c "import bcrypt; print(bcrypt.hashpw(\"$pwd\", bcrypt.gensalt(prefix=b\"2a\")))"`
command line. Default password is 'password'.

Also you can define a list of users (see above sample). Each user requires only the password field. Note that other roles may be specified without `ROLE_` prefix. If a role contains '@', then the word after it is interpreted as the tenant. 

### User management ###

You can access the user list through `SecurityApi`. It is described in 
[swagger](http://172.31.0.3:8761/swagger-ui.html#/security-api). We show only a subset of methods.

If you has the 'ROLE_ADMIN' then you can access to all methods. Otherwise, you can invoke only '/ui/api/users-current' and '/ui/api/roles/' to see own roles.

Also you can access 'POST /ui/api/users/{user}' only when `{user}` - equals with name of current user.  
Afterward, You may change all properties except roles (it can only be changed by the admin), name, and tenant.  

The APIs '/ui/api/users-current' and '/ui/api/users/{user}' provides a response such as one below:

    {
      "user": "second",
      "title": "Mr. Second",
      "email": "se@co.nd",
      "tenant": "root",
      "password": "********",
      "accountNonExpired": null,
      "accountNonLocked": null,
      "credentialsNonExpired": null,
      "enabled": null,
      "roles": [
        {
          "name": "ROLE_GC",
          "tenant": "java"
        },
        {
          "name": "ROLE_DEVELOPER",
          "tenant": "root"
        }
      ]
    }

For modify a user, you must invoke '/ui/api/users/{user}' with a set of modified fields. For example, when you need
to change title and user email, the request would be:

    POST /ui/api/users/second
    {
      "title": "Mr. Second",
      "email": "se@co.nd",
    }

When you want to change a set of user roles, you can invoke 'POST /ui/api/users/{user}/roles/'.
For example, if we want to remove `ROLE_GC` and add `ROLE_ALLOCATOR`, we make the following request:

    POST /ui/api/users/{user}/roles/
    [
      {
        "name": "ROLE_GC",
        "tenant": "java",
        "delete": true
      },
      {
        "name": "ROLE_DEVELOPER",
        "tenant": "root"
      }
    ]

## Authentication ##

The system supports BasicAuth and token-based authentication. The token is obtain by login/password pair. There is no  
default timeout of the toke each token usage can configured the timeout.

### Token ###

The token can be obtain through `/ui/token/login` API:

    curl -X POST --header 'Content-Type: application/json' \
    --data-raw '{"username":"admin","password":"password"}' \
    'http://cluman.server:8761/ui/token/login'

It return the JSON object with the following format:

    {
      "userName": "admin",
      "key": "sit:MAA..TL;DR..KO===",
      "creationTime": "2016-11-16T15:44:24.266",
      "expireAtTime": "2016-11-17T15:44:24.266"
    }

The response has the 'key' with the token data and should be placed in 'X-Auth-Token' header. 
The token will expire at `expireAtTime` (`dm.token.expireAfterInSec` in config), but each request with token
will prolong token lifetime to `expireLastAccessInSec` value (`dm.token.expireAfterInSec` in config).   

For getting new token you can invoke `/ui/token/refresh`:

    curl -X PUT -H 'X-Auth-Token: sit:MAA..TL;DR..KO===' 'http://cluman.server:8761/ui/token/refresh'

It return same result as `/ui/token/login`.

## Authorization ##

The authorization system is based on user roles and Access Control Lists (ACL).

The system stores predefined roles in `RoleHierarchyImpl`, note that it has only 'ROLE_ADMIN' and 'ROLE_USER'.

The ACL uses following objects:

* Principal - simply user.
* Granted Authority - usually it is role and user may has many roles. Some roles may be predefined but the system support adding custom roles.
* ObjectIdentifier (OID) - identifier of object and can save it as string like `<TYPE>':s:'<id>`
* Security Identity (SID) - identifier of principal (user) or the granted authority  
* Access Control List (ACL) - contains the oid, owner SID, and list of ACEs
* Access Control Entry (ACE) - entry which contains the SID, permission and grant/revoke flag. Also has ID which is 
used only for manipulation by them.
* Permission - presented as `PermissionData` object, which can be built from primitive permissions, 
see `com.codeabovelab.dm.common.security.Action`. Each primitive permission can be a single char in string, now set of 
all permissions:

 - CREATE - create object
 - READ - read object
 - UPDATE - modify object (ieg change cluster title)
 - DELETE - delete object
 - EXECUTE - execute object (ieg run job)
 - ALTER_INSIDE - alter something inside object (ieg add node to cluster)

Permissions may be saved as string: all - 'CRUDEA', set of READ and CREATE - 'CR' and etc. order of letters has no effect.    

An OID is formed by two components: type (String) and id (String, Integer, Long).

The type can be one of following value:

  - CLUSTER
  - NODE - inherited from cluster, when node is not connected to cluster it accessible to any `ROLE_USER`
  - CONTAINER - inherited from cluster
  - LOCAL_IMAGE - not used
  - REMOTE_IMAGE - not used
  - NETWORK - not used

Its list may change in future, see `SecuredType` for actual list. 

System has `ConfigurableAclService` which allow ACL configuration through config, but not currently used.
Actual ACL service is presented by `ProvidersAclService`, which combine ACLs from a set of providers. We have three 
providers currently: `ClusterAclProvider`, `ContainersAclProvider` and `NodesAclProvider`. 
First provider stores the ACL list into KV-storage, others - resolves 'cluster' for its secured object 
and generate appropriate ACL. You can obtain ACLs for this types but not change it.

Example of ACLs: 

For cluster: 

    GET /ui/api/acl/CLUSTER/testcluster
    {
      "objectIdentity": "CLUSTER:s:testcluster",
      "owner": {
        "type": "PRINCIPAL",
        "principal": "system",
        "tenant": "root"
      },
      "parentAcl": null,
      "entriesInheriting": false,
      "entries": []
    }

Its default ACL is not persisted and is generated in runtime.

For node (autogenerated from cluster):

    GET /ui/api/acl/NODE/docker-exp2
    {
      "objectIdentity": "NODE:s:docker-exp2",
      "owner": {
        "type": "PRINCIPAL",
        "principal": "system",
        "tenant": "root"
      },
      "parentAcl": null,
      "entriesInheriting": false,
      "entries": []
    }

For adding entry for user 'second' with read (`R`) permission:

    POST /ui/api/acl/CLUSTER/testcluster
    {
      "entries": [
        { 
          "id":"1",
          "sid": {
            "type": "PRINCIPAL",
            "principal": "second",
            "tenant":"root"
          },
          "granting": true,
          "permission": "R"
        }
      ]
    }

Afterward, the cluster has following ACL:

    GET /ui/api/acl/CLUSTER/testcluster
    {
      "objectIdentity": "CLUSTER:s:testcluster",
      /* nothing changed here */
      "entries": [
        {
          "id": "1",
          "sid": {
            "type": "PRINCIPAL",
            "principal": "second",
            "tenant": "root"
          },
          "granting": true,
          "permission": "R",
          "auditFailure": false,
          "auditSuccess": false
        }
      ]
    }

For node(autogenerated from cluster):

    GET /ui/api/acl/NODE/docker-exp2
    {
      "objectIdentity": "NODE:s:docker-exp2",
      /* nothing changed here */
      "entries": [
        {
          "id": "1",
          "sid": {
            "type": "PRINCIPAL",
            "principal": "second",
            "tenant": "root"
          },
          "granting": true,
          "permission": "R",
          "auditFailure": false,
          "auditSuccess": false
        }
      ]
    }

It provides the same permission format as cluster but differ in `READ` permission, other permissions from 'CUDE'. Otherwise nothing is changed. 
But `ALTER_INSIDE` - grant all modification permissions of node to user. 
Below we change entry of ACL for adding permission, note that `entry.id` must be same, otherwise a new entry will be created.

    POST /ui/api/acl/CLUSTER/testcluster
    {
      "entries": [
        { 
          "id":"1",
          "permission": "AR"
        }
      ]
    }

And cluster ACL looks like:

    GET /ui/api/acl/CLUSTER/testcluster
    {
      "objectIdentity": "CLUSTER:s:testcluster",
      /* nothing changed here */
      "entries": [
        {
          "id": "1",
          "sid": {
            "type": "PRINCIPAL",
            "principal": "second",
            "tenant": "root"
          },
          "granting": true,
          "permission": "RA",
          "auditFailure": false,
          "auditSuccess": false
        }
      ]
    }

And node:

    GET /ui/api/acl/NODE/docker-exp2
    {
      "objectIdentity": "NODE:s:docker-exp2",
      /* nothing changed here */
       "entries": [
        {
          "id": "1",
          "sid": {
            "type": "PRINCIPAL",
            "principal": "second",
            "tenant": "root"
          },
          "granting": true,
          "permission": "CRUDEA",
          "auditFailure": false,
          "auditSuccess": false
        }
      ]
    }

See that user 'second' is given all the permissions on node.

And at end we delete ACL entry with following request:

    POST /ui/api/acl/CLUSTER/testcluster
    {
      "entries": [
        { 
          "id":"1",
          "delete":true
        }
      ]
    }

Cluster ACL now do not have any entries:

    GET /ui/api/acl/CLUSTER/testcluster
    {
      "objectIdentity": "CLUSTER:s:testcluster",
      "owner": {
        "type": "PRINCIPAL",
        "principal": "system",
        "tenant": "root"
      },
      "parentAcl": null,
      "entriesInheriting": false,
      "entries": []
    }

