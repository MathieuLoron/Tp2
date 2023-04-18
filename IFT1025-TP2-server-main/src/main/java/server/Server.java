package server;


import client.EventHandler;
import javafx.util.Pair
import server.models.Course;
import server.models.RegistrationForm;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * La classe serveur
 */
public class Server {
    /**
     *
     */
    public final static String REGISTER_COMMAND = "INSCRIRE";
    /**
     *
     */
    public final static String LOAD_COMMAND = "CHARGER";
    private final ServerSocket server;
    private Socket client;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private final ArrayList<EventHandler> handlers;

    /**
     * Une exception
     * @param port Endroit ou le serveu est heberger
     * @throws IOException
     */
    public Server(int port) throws IOException {
        this.server = new ServerSocket(port, 1);
        this.handlers = new ArrayList<EventHandler>();
        this.addEventHandler(this::handleEvents);
    }

    /**
     * ajoute une commande
     * @param h commande ajoutée
     */
    public void addEventHandler(EventHandler h) {
        this.handlers.add(h);
    }

    private void alertHandlers(String cmd, String arg) {
        for (EventHandler h : this.handlers) {
            h.handle(cmd, arg);
        }
    }

    /**
     * permet de gerer le client
     */
    public void run() {
        while (true) {
            try {
                client = server.accept();
                System.out.println("Connecté au client: " + client);
                objectInputStream = new ObjectInputStream(client.getInputStream());
                objectOutputStream = new ObjectOutputStream(client.getOutputStream());
                listen();
                disconnect();
                System.out.println("Client déconnecté!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Attend la prochaine commande
     * @throws IOException les exceptions
     * @throws ClassNotFoundException une exception quand une classe rechercher n'est pas trouver
     */
    public void listen() throws IOException, ClassNotFoundException {
        String line;
        if ((line = this.objectInputStream.readObject().toString()) != null) {
            Pair<String, String> parts = processCommandLine(line);
            String cmd = parts.getKey();
            String arg = parts.getValue();
            this.alertHandlers(cmd, arg);
        }
    }

    /**
     *
     * creer des paires de commande et d'arguments
      * @param line Commande total de commande et arguments
     * @return retourne la paire
     */
    public Pair<String, String> processCommandLine(String line) {
        String[] parts = line.split(" ");
        String cmd = parts[0];
        String args = String.join(" ", Arrays.asList(parts).subList(1, parts.length));
        return new Pair<>(cmd, args);
    }

    /**
     * Deconnecte le client du serveur
     * @throws IOException
     */
    public void disconnect() throws IOException {
        objectOutputStream.close();
        objectInputStream.close();
        client.close();
    }

    public void handleEvents(String cmd, String arg) {
        if (cmd.equals(REGISTER_COMMAND)) {
            handleRegistration();
        } else if (cmd.equals(LOAD_COMMAND)) {
            handleLoadCourses(arg);
        }
    }

    /**
     * Lire un fichier texte contenant des informations sur les cours et les transofmer en liste d'objets 'Course'.
     * La méthode filtre les cours par la session spécifiée en argument.
     * Ensuite, elle renvoie la liste des cours pour une session au client en utilisant l'objet 'objectOutputStream'.
     * La méthode gère les exceptions si une erreur se produit lors de la lecture du fichier ou de l'écriture de l'objet dans le flux.
     *
     * @param arg la session pour laquelle on veut récupérer la liste des cours
     */
    public void handleLoadCourses(String arg) {
        try {
            // Lire le fichier texte contenant les informations sur les cours
            BufferedReader bufferedReader = new BufferedReader(new FileReader("cours.txt"));
            ArrayList<Course> courses = new ArrayList<>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                // Diviser la ligne en différents attributs du cours
                String[] parts = line.split("\t");
                String session = parts[0].trim();
                String code = parts[1].trim();
                String nom = parts[2].trim();
                // Vérifier si la session correspond à celle spécifiée en argument
                if (session.equals(arg)) {
                    // Créer un objet Course et l'ajouter à la liste des cours pour cette session
                    Course course = new Course(session, code, nom);
                    courses.add(course);
                }
            }
            bufferedReader.close();

            // Envoyer la liste des cours pour cette session au client
            objectOutputStream.writeObject(courses);
            objectOutputStream.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Récupérer l'objet 'RegistrationForm' envoyé par le client en utilisant 'objectInputStream', l'enregistrer dans un fichier texte
     * et renvoyer un message de confirmation au client.
     * La méthode gére les exceptions si une erreur se produit lors de la lecture de l'objet, l'écriture dans un fichier ou dans le flux de sortie.
     */
    public void handleRegistration() {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("registration.obj"));
            RegistrationForm registrationForm = (RegistrationForm) objectInputStream.readObject();
            objectInputStream.close();

            BufferedWriter writer = new BufferedWriter(new FileWriter("registrations.txt", true));
            writer.write(registrationForm.toString());
            writer.newLine();
            writer.close();

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("confirmation.obj"));
            objectOutputStream.writeObject("Registration successful");
            objectOutputStream.close();
        } catch (IOException e) {
            System.err.println("Error reading or writing file.");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Error reading object.");
            e.printStackTrace();
        }
    }
}


