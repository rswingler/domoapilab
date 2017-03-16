# domoapilab
Domopalooza

# Lab Steps

## Step 1 - Get Access Token

http -v POST https://api.domo.com/oauth/token grant_type==client_credentials scope=="data user" -a <client id>:<client secret> --pretty=format

## Step 2 - Save Token as variable

export ACCESS_TOKEN=<access token>

## Step 3 - Call user API (Get a User)

http -v https://api.domo.com/v1/users/669096686 Authorization:"bearer $ACCESS_TOKEN" --pretty=format

## Step 4 - Create a dataset

echo '{ "name": "Sample Data", "description": "Just some data", "schema": { "columns":[{ "type":"STRING", "name":"First" }] }}' \
 | http -v POST https://api.domo.com/v1/datasets Authorization:"bearer $ACCESS_TOKEN" --pretty=format

(Note the returned DataSet ID for future requests)

## Step 5 - Fetch a dataset

http -v https://api.domo.com/v1/datasets/<dataset id> Authorization:"bearer $ACCESS_TOKEN" --pretty=format

## Step 6 - Update a dataset's schema

echo '{ "name": "Sample Data", "description": "Just some data now with 3 columns", 
 "schema": { "columns":[{ "type":"STRING", "name":"1st Letters" }, { "type":"STRING", "name":"2nd Letters" }, { "type":"STRING", "name":"3rd Letters" }] }}' \
  | http -v PUT https://api.domo.com/v1/datasets/<dataset id> Authorization:"bearer $ACCESS_TOKEN" --pretty=format

## Step 7 - Upload data

echo '"a","b","c"
"d","e","f"
"g","h","i"
"j","k","l"
"m","n","o"
"p","q","r"' \
  | http -v PUT https://api.domo.com/v1/datasets/<dataset id>/data Content-Type:"text/csv" Authorization:"bearer $ACCESS_TOKEN" --pretty=format

<pre>
