## Concept ##

System use Access Control List (ACL) extended to use roles (GrantedAuthorities in spring terms) as security model. 
Also it based on [spring ACL](http://docs.spring.io/spring-security/site/docs/current/reference/html/domain-acls.html).

Each user presented as `ExtendedUserDetails` and has list of roles. Any authenticated user has 'ROLE_USER' too.

User is stored in config file or KV-storage. Each case managed by different services: `ConfigurableUserDetailService` or 
`UsersStorage`. System combine its through `CompositeUserDetailsService`. 

You can see 'tenant' field in different security objects - it needed in multitenancy environment, support of this is 
planning for future releases, at now you must fill those fields with 'root' - value (`MultiTenancySupport.ROOT_TENANT`).

### Configuration ###

Config sample:

    dm.auth.adminPassword=$2a$08$bFLBfYL8Eb6n71D/yvLyLu9QzxDWEPG0TTx3/LgfiwaKdhfyCEdVe
    dm.auth.user[second].password=$2a$08$bFLBfYL8Eb6n71D/yvLyLu9QzxDWEPG0TTx3/LgfiwaKdhfyCEdVe
    dm.auth.user[second].email=se@co.nd
    dm.auth.user[second].title=Mr. Second
    dm.auth.user[second].roles=DEVELOPER, GC@java 

Password is bcrypt hash, it may be created by 
`read pwd && python -c "import bcrypt; print(bcrypt.hashpw(\"$pwd\", bcrypt.gensalt(prefix=b\"2a\")))"`
command line. Default password is 'password'.

Also you can defile list of users (see above sample), each user require only password field. Note than roles may be 
specified without `ROLE_` prefix. If role contains '@' - then word after it interpret as tenant. 

### User management ###

So you can access to user list through `SecurityApi`, it described in 
[swagger](http://172.31.0.3:8761/swagger-ui.html#/security-api). We show here only 
small piece of it methods set.

If you has 'ROLE_ADMIN' then obviously you can access to all methods, 
otherwise you can invoke only '/ui/api/users-current' and '/ui/api/roles/' to see own roles.

'/ui/api/users-current' and '/ui/api/users/{user}' give response like:

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

Note that it newer show real password, anytime you can see only '********' string.

For modify user you must invoke '/ui/api/users/{user}' with set of modified fields. For example, when you want 
to change title and email of user, you must send:

    POST /ui/api/users/second
    {
      "title": "Mr. Second",
      "email": "se@co.nd",
    }

When you want to change set of user roles, you can invoke 'POST /ui/api/users/{user}/roles/',
for example if we want to remove `ROLE_GC` and add `ROLE_ALLOCATOR`, we must make following request:

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

Note that, when user has adding role - the newer is changed.

## Authentication ##

System support BasicAuth and token-based authentication. Token is obtain by login-password pair. Token has 
lifetime, also each token usage update lifetime on configured timeout.

### Token ###

Token can be obtain through `/ui/token/login` entry point:

    curl -X POST --header 'Content-Type: application/json' \
    --data-raw '{"username":"admin","password":"password"}' \
    'http://cluman.server:8761/ui/token/login'

It return json object like:

    {
      "userName": "admin",
      "key": "sit:MAA..TL;DR..KO===",
      "creationTime": "2016-11-16T15:44:24.266",
      "expireAtTime": "2016-11-17T15:44:24.266"
    }

Response has 'key' with token data. So you must place it key in 'X-Auth-Token' header. 
Note that toke will expire at `expireAtTime` (`dm.token.expireAfterInSec` in config), but each request with token
will prolong token lifetime to `expireLastAccessInSec` value (`dm.token.expireAfterInSec` in config).   

For getting new token you can invoke `/ui/token/refresh`:

    curl -X PUT -H 'X-Auth-Token: sit:MAA..TL;DR..KO===' 'http://cluman.server:8761/ui/token/refresh'

It return same result as `/ui/token/login`.

## Authorization ##

Authorization based on user roles and Access Control Lists.

Acl use following objects:

* Principal - simply user. 
* Granted Authority - usual it is role, user may has many roles. Some roles may be predefined, but system support adding custom roles.
* ObjectIdentifier (OID) - identifier of object, it object, but we can save it as string like `<TYPE>':s:'<id>`
* Security Identity (SID) - identifier of principal (user) or granted authority.  
* Access Control List (ACL) - contains oid, owner SID and list of ACEs.
* Access Control Entry (ACE) - entry which contain SID, permission and grant/revoke flag, it also has id - which is 
used only for manipulation by them.

