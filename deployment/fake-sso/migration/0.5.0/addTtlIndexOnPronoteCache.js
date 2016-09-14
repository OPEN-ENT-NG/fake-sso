//12 H
db.pronotecache.createIndex( { "insertedAt": 1 }, { expireAfterSeconds: 43200 } );