package com.klix.app;

import com.beust.jcommander.JCommander;
import com.klix.app.db.KlixModel;
import com.klix.app.db.Model;
import com.klix.app.services.link.RedirectHandler;
import com.klix.app.services.link.ShortenHandler;
import com.klix.app.utils.CommandLineOptions;
import com.sun.org.apache.bcel.internal.generic.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import spark.Spark;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ThreadPoolExecutor;

import static spark.Spark.*;


public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws URISyntaxException {
        CommandLineOptions options = new CommandLineOptions();
        new JCommander(options, args);
        log.info("Options.debug = " + options.debug);
        log.info("Options.database = " + options.database);
        log.info("Options.dbHost = " + options.dbHost);
        log.info("Options.dbUsername = " + options.dbUsername);
        log.info("Options.dbPort = " + options.dbPort);
        log.info("Options.servicePort = " + options.servicePort);

        port(options.servicePort);


        URI dbUri = new URI(System.getenv("DATABASE_URL"));

        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();


        Sql2o sql2o = new Sql2o(
                dbUrl,
                username,
                password);

        try(org.sql2o.Connection cn = sql2o.open()){
            cn.createQuery("select * from roles").executeAndFetch(Object.class);
        } catch (Exception ex){
            ex.printStackTrace();
            halt("Can't connect to db: ");

        }

        Model model = new KlixModel(sql2o);

        /***********************/

        Spark.staticFileLocation("/public");


        ShortenHandler shortenHandler = new ShortenHandler(model, options.serviceHost, options.servicePort);
        RedirectHandler redirectHandler = new RedirectHandler(model, options.serviceHost, options.servicePort);
        post("/shorten", shortenHandler);
        get("/:id", redirectHandler);
    }
}
