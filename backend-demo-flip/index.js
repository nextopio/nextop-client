var fs = require('fs');
var express = require('express');
var bodyParser = require('body-parser');
var mysql = require('mysql');


var configObj;
try {
    configObj = JSON.parse(fs.readFileSync('./config.json'));
    console.dir(configObj);
}
catch (err) {
    console.log(err);
}


/////// MYSQL ///////

var mysqlClient;
setMysqlClient(configObj.mysql);

function setMysqlClient(config) {
    mysqlClient = mysql.createConnection(config);
    // recover from broken connections
    mysqlClient.connect(function (err) {
        if (err) {
            if ('PROTOCOL_CONNECTION_LOST' == err.code) {
                setMysqlClient(config);
            } else if (err.fatal) {
                throw err;
            }
        }
    });
}



/////// ROUTING + HTTP ///////

var feedId = '0000000000000000000000000000000000000000000000000000000000000000';

var app = express();
app.use(bodyParser.json());


/* @see http://expressjs.com/api.html
 */

// ?count= for top count
// ?before= to go backward from index
app.get('/feed', function (req, res) {

    var count = req.query.count || 50;
    if ('before' in req.query) {
        var before = req.query.before;
        mysqlClient.query('SELECT flip_id, most_recent_update_index FROM Feed' +
            ' WHERE feed_id = ? AND deleted = false AND most_recent_update_index < ?' +
            ' ORDER BY most_recent_update_index DESC' +
            ' LIMIT ?',
            [feedId, before, count],
            function(err, result) {
                if (err) throw err;

                res.status(200);
                res.json(result);
                res.end();
            });
    } else if ('after' in req.query) {
        var after = req.query.after;
        mysqlClient.query('SELECT flip_id, most_recent_update_index FROM Feed' +
            ' WHERE feed_id = ? AND deleted = false AND most_recent_update_index > ?' +
            ' ORDER BY most_recent_update_index ASC' +
            ' LIMIT ?',
            [feedId, after, count],
            function(err, result) {
                if (err) throw err;

                res.status(200);
                res.json(result);
                res.end();
            });
    } else {
        mysqlClient.query('SELECT flip_id, most_recent_update_index FROM Feed' +
            ' WHERE feed_id = ? AND deleted = false AND most_recent_update_index > 0' +
            ' ORDER BY most_recent_update_index DESC' +
            ' LIMIT ?',
            [feedId, count],
            function(err, result) {
                if (err) throw err;

                res.status(200);
                res.json(result);
                res.end();
            });
    }

});

// ?after= to go after index
app.get('/flip/:flipId/info', function (req, res) {
    var flipId = req.params.flipId;
    if ('after' in req.query) {
        var after = req.query.after;
        mysqlClient.query('SELECT intro, most_recent_update_index FROM FlipInfo' +
            ' WHERE flip_id = ? AND deleted = false AND most_recent_update_index > ?',
            [flipId, after],
            function(err, result) {
                if (err) throw err;

                res.status(200);
                res.json(result);
                res.end();
            });
    } else {
        mysqlClient.query('SELECT intro, most_recent_update_index FROM FlipInfo' +
            ' WHERE flip_id = ? AND deleted = false AND most_recent_update_index > 0',
            [flipId],
            function(err, result) {
                if (err) throw err;

                res.status(200);
                res.json(result);
                res.end();
            });
    }
});

// ?after= to go after index
app.get('/flip/:flipId/frame', function (req, res) {
    var flipId = req.params.flipId;
    if ('after' in req.params) {
        var after = req.query.after;
        mysqlClient.query('SELECT frame_id, creation_time, image_url, most_recent_update_index FROM FlipFrame' +
            ' WHERE flip_id = ? AND deleted = false AND most_recent_update_index > ?' +
            ' ORDER BY most_recent_update_index ASC',
            [flipId, after],
            function(err, result) {
                if (err) throw err;

                res.status(200);
                res.json(result);
                res.end();
            });
    } else {
        mysqlClient.query('SELECT frame_id, creation_time, image_url, most_recent_update_index FROM FlipFrame' +
            ' WHERE flip_id = ? AND deleted = false AND most_recent_update_index > 0' +
            ' ORDER BY most_recent_update_index ASC',
            [flipId],
            function(err, result) {
                if (err) throw err;

                res.status(200);
                res.json(result);
                res.end();
            });
    }
});


app.put('/flip/:flipId', function (req, res) {
    var flipId = req.params.flipId;

    mysqlClient.query('INSERT IGNORE INTO Feed (feed_id, flip_id) VALUES (?, ?)',
        [feedId, flipId],
        function(err, result) {
            if (err) throw err;
        });

    bumpFeed(feedId, flipId);

    res.status(200);
    res.end();
});

app.post('/flip/:flipId/info', function (req, res) {
    var flipId = req.params.flipId;

    if ('intro' in req.body) {
        var intro = req.body.intro;
        mysqlClient.query('INSERT INTO FlipInfo (flip_id, intro) VALUES (?, ?) ON DUPLICATE KEY UPDATE intro = ?',
            [flipId, intro, intro],
            function(err, result) {
                if (err) throw err;
            });

        bumpFlipInfo(flipId);
    }

    res.status(200);
    res.end();
});

// ?creation_time=  set creation time
// post body is the image of the frame
app.put('/flip/:flipId/frame/:frameId', function (req, res) {
    var flipId = req.params.flipId;
    var frameId = req.params.frameId;

    var creationTime = req.query.creation_time || 0;

    // FIXME error handling here
    req.pipe(fs.createWriteStream('./tmp/' + flipId + '_' + frameId + '.jpeg'));
    var imageUrl = 'http://demo-flip.nextop.io/flip/' + flipId + '/frame/' + frameId;

    // update creation_time, image_url in DB
    mysqlClient.query('INSERT INTO FlipFrame VALUES (flip_id, frame_id, creation_time, image_url) VALUES (?, ?, ?, ?)' +
        ' ON DUPLICATE KEY UPDATE creation_time = ?, image_url = ?',
        function (err, result) {
            if (err) throw err;

            res.status(200);
            res.end();
        });

});

app.get('/flip/:flipId/frame/:frameId', function (req, res) {
    var flipId = req.params.flipId;
    var frameId = req.params.frameId;
    res.status(200);
    res.set('Content-Type', 'image/jpeg');
    fileSystem.createReadStream('./tmp/' + flipId + '_' + frameId + '.jpeg').pipe(res);
    res.end();
});




var server = app.listen(3770, function () {
    var host = server.address().address;
    var port = server.address().port;

    console.log('demo-flip listening at http://%s:%s', host, port)
});


/////// CONTROLLERS ///////

/* bump surfaces the value in the updates feed */

function bumpFeed(feedId, flipId) {
    mysqlClient.query('INSERT IGNORE INTO FeedUpdates (feed_id) VALUES (?)',
        [feedId],
        function(err, result) {
            if (err) throw err;
        });
    mysqlClient.query('UPDATE Feed, FeedUpdates SET FeedUpdates.update_index = FeedUpdates.update_index + 1, Feed.most_recent_update_index = FeedUpdates.update_index + 1' +
        ' WHERE FeedUpdates.feed_id = ? AND Feed.feed_id = ? AND Feed.flip_id = ?',
        [feedId, feedId, flipId],
        function(err, result) {
            if (err) throw err;
        });
}

function bumpFlipInfo(flipId) {
    mysqlClient.query('UPDATE FlipInfo SET update_index = update_index + 1 WHERE flip_id = ?',
        [flipId],
        function(err, result) {
            if (err) throw err;
        });
}

function bumpFlipFrame(flipId, frameId) {
    mysqlClient.query('INSERT IGNORE INTO FlipFrameUpdates (flip_id) VALUES (?)',
        [flipId],
        function(err, result) {
            if (err) throw err;
        });
    mysqlClient.query('UPDATE FlipFrame, FlipFrameUpdates SET FlipFrameUpdates.update_index = FlipFrameUpdates.update_index + 1, FlipFrame.most_recent_update_index = FlipFrameUpdates.update_index + 1' +
        ' WHERE FlipFrameUpdates.flip_id = ? AND FlipFrame.flip_id = ? AND FlipFrame.frame_id = ?',
        [flipId, flipId, frameId],
        function(err, result) {
            if (err) throw err;
        });
}
