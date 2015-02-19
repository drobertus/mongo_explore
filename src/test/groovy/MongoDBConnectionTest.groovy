import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import groovy.util.logging.Log
import spock.lang.Specification

import java.text.SimpleDateFormat;

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue;

@Log
class MongoDBConnectionTest extends Specification {


    def "test basic connection"() {
        expect:
// To directly connect to a single MongoDB server (note that this will not auto-discover the primary even
// if it's a member of a replica set:

// or
        MongoClient mongoClient = new MongoClient("localhost", 27017);
// or, to connect to a replica set, with auto-discovery of the primary, supply a seed list of members
        //MongoClient mongoClient = new MongoClient(Arrays.asList(new ServerAddress("localhost", 27017),
        //   new ServerAddress("localhost", 27018),
        //   new ServerAddress("localhost", 27019)));
//        def dbName = 'mydb'
//        DB db = mongoClient.getDB(dbName);
//
//        String name = db.getName()
//        assertEquals dbName, name
//
//        Set<String> knowColls = db.collectionNames
//        knowColls.each {
//            println it
//        }
//
//        DBCollection coll = db.getCollection('testData')
//        println("count is ${coll.count()} in testData");
//        DBCursor cur1 = coll.find()
//        while(cur1.hasNext()) {
//            println("val= ${cur1.next()}")
//        }
//        cur1.close()


        //TODO: test inserts, updated, deletes

        //create test db,
        def testDBName = 'theTestDB'

        //have some cleanup if this structure exists
        def dbs = mongoClient.getDatabaseNames()
        if (dbs.contains(testDBName)) {
            mongoClient.dropDatabase(testDBName)
        }

        DB testDB = mongoClient.getDB(testDBName)
        assertEquals(testDBName, testDB.getName())

        //create test collection(s)
        def testCollName = 'testCollection'
        DBObject options = new BasicDBObject("capped", true)
            .append("size", 1048576)

        def testColls = testDB.getCollectionNames()
        assertEquals(0, testColls.size())
        DBCollection testColl = testDB.createCollection(testCollName, options)
        def testColls2 = testDB.getCollectionNames()
        assertEquals(2, testColls2.size())
        testColls2.each() {
            println it
        }
        assertTrue(testColls2.contains(testCollName))

        //now lets add, update and delete a document in the collection
        assertEquals 0, testColl.count()

        def sdf = new SimpleDateFormat('yyyy-MM-dd')
        def mapOfVals = [:]
        mapOfVals <<  ['id': 25]
        mapOfVals << ['lastName':'Sanderson']
        mapOfVals << ['firstName': 'Thomas']
        mapOfVals << ['dob': sdf.parse('2005-07-12')]

        testColl.insert(new BasicDBObject(mapOfVals))

        def bDate2 = sdf.parse('1968-03-22')
        def obj2 = new BasicDBObject('id', 34).append('lastName', 'Thorn')
            .append('firstName', 'Marilyn').append('dob', bDate2)
            .append('birthNumber', 5.89d)
        testColl.insert(obj2)

        def testDate = sdf.parse('1999-12-31')

        assertEquals 2, testColl.count()
        DBCursor cursor = testColl.find()
        def countReturned = 0
        while(cursor.hasNext()) {
            countReturned ++
            BasicDBObject theDoc = cursor.next()
            println countReturned + ': ' + theDoc.toString()
            Date dob = theDoc.get('dob')
            if (dob.after(testDate)) {
              println "${dob} is after ${testDate}"
            }
            else {
                println "${dob} is before ${testDate}"
            }
        }
        cursor.close()
        def finder1 = testColl.find(new BasicDBObject('id', 34))
        def countReturned2 = 0
        BasicDBObject foundDoc
        while(finder1.hasNext()) {
            countReturned2 ++
            foundDoc = finder1.next()
            println countReturned2 + ': ' + foundDoc.toString()
        }
        finder1.close()
        assertEquals (1, countReturned2)
        assertEquals('Thorn', foundDoc.get('lastName'))
        assertEquals('Marilyn', foundDoc.get('firstName'))


        // indexes

        //create ascending index on last name
        testColl.createIndex(new BasicDBObject('lastName', 1))
        def indexInfo = testColl.getIndexInfo()
        indexInfo.each {
            println it
        }


        mongoClient.dropDatabase(testDBName)

        def remainingDBs = mongoClient.databaseNames

        assertFalse( remainingDBs.contains(testDBName) )

        mongoClient.close()
    }

}