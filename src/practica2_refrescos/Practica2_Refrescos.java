/*
 * https://drive.google.com/file/d/1hgBbi4YEr7E_zTfm78qd57TyXKOkA_dg/view?usp=sharing
 * Autor: Dilpreet Singh
 */

package practica2_refrescos;

import java.util.Random;
import static practica2_refrescos.Practica2_Refrescos.CONSUMERS;
import static practica2_refrescos.Practica2_Refrescos.PRODUCERS;
import static practica2_refrescos.Practica2_Refrescos.TO_CONSUME;
import static practica2_refrescos.Practica2_Refrescos.acabats;

public class Practica2_Refrescos {

    final static int BUFFER_SIZE = 10; //Tamaño del buffer
    static int PRODUCERS; // Número de productores
    static int CONSUMERS; //Número de consumidores
    static int TO_CONSUME[]; //Array para asignar operaciones a cada consumidor
    static volatile int acabats = 0; //Número de clientes acabados

    public static void main(String[] args) throws InterruptedException {
        Random ran = new Random();
        PRODUCERS = ran.nextInt(5); //Número aleatorio de productores
        CONSUMERS = ran.nextInt(7); //Número aleatorio de consumidores

        TO_CONSUME = new int[CONSUMERS];

        for (int i = 0; i < CONSUMERS; i++) {
            TO_CONSUME[i] = ran.nextInt(10); //Número aleatorio de operaciones para cada cliente
        }

        System.out.println("COMENÇA LA SIMULACIÓ");
        System.out.println("Avui hi ha " + CONSUMERS + " consumidors i " + PRODUCERS + " reposadors");
        System.out.println("La màquina de refrescs està buida, hi caben " + BUFFER_SIZE + " refrescs");

        Thread[] threads = new Thread[PRODUCERS + CONSUMERS];
        Maquina monitor = new Maquina(BUFFER_SIZE);
        int i;
        int t = 0;
        for (i = 0; i < CONSUMERS; i++) {
            threads[t] = new Thread(new Consumer(i, monitor, TO_CONSUME[i]));
            threads[t].start();
            t++;
        }
        Thread.sleep(100);
        for (i = 0; i < PRODUCERS; i++) {
            threads[t] = new Thread(new Producer(i, monitor));
            threads[t].start();
            t++;
        }
        t = 0;
        for (i = 0; i < CONSUMERS; i++) {
            threads[t].join();
            t++;
        }
        for (i = 0; i < PRODUCERS; i++) {
            threads[t].join();
            t++;
        }
        System.out.println("ACABA LA SIMULACIÓ");
    }

}

class Maquina {

    private volatile int elements = 0;
    private int size;
    private volatile boolean reposant = false;

    public Maquina(int size) {
        this.size = size;
    }

    synchronized void reposar(int id) throws InterruptedException {
        //si la máquina está llena y aún faltan clientes por acabar, entonces se
        // quedaran bloqueados. Si todos los clientes han acabado, los productores
        // no deben de ser bloqueados aquí
        while (elements == size && acabats < CONSUMERS) {
            this.wait();
        }
        //Si han terminado todos los clientes, pueden haber bloqueados productores
        //en el while anterior. Para liberarlos hacemos un notifyAll
        if (acabats == CONSUMERS) {
            this.notifyAll();
        }
        //Si han terminado todos los clientes y la máquina está llena, los produc-
        //tores no tienen que reponer
        if (elements < size) {
            reposant = true;
            System.out.println("El reposador " + id + " reposa la màquina, hi ha " + (elements) + " refrescs i en posa " + (size - elements));
            elements = size; //llenamos
            reposant = false;
        }
        this.notifyAll();
    }

    synchronized int agafar(int id, int counter, int operations) throws InterruptedException {
        //si la máquina está vaciá Y aún no han terminado los consumidores Y,
        //no hay productores reponiendo, el consumidor quedará bloqueado
        while (elements == 0 && acabats < CONSUMERS || reposant) {
            this.wait(); 
        }
        //Al ser liberado decrementará en una unidad los elementos de la máquina
        System.out.println("\tEl consumidor " + id + " agafa un refresc - consumició: " + (counter + 1));
        elements--;
        
        if (operations == counter + 1) { //si ha completado sus operaciones
            acabats++;
        }
        this.notifyAll();
        return 1;
    }

    //Este método synchronized nos servirá para aumentar el número de clientes
    //acabados que tenían 0 consumiciones
    synchronized void acaba() {
        acabats++;
    }
}

class Producer implements Runnable {

    Maquina monitor;
    int id;

    public Producer(int id, Maquina monitor) {
        this.id = id;
        this.monitor = monitor;
    }

    @Override
    public void run() {
        System.out.println("Arriba reposador " + id);
        //Mientras no hayan terminado todos los clientes los reponedores hacen su trabajo
        while (acabats < CONSUMERS) {
            try {
                monitor.reposar(id); //reponen, llaman al método del monitor
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
            }
        }
        System.out.println("Productor " + id + " ha acabat");
    }
}

class Consumer implements Runnable {

    Maquina monitor;
    private int operations; //el número de consumiciones que tiene que hacer
    private volatile int counter = 0; //contador de elementos consumidos
    int id;

    public Consumer(int id, Maquina monitor, int ops) {
        this.id = id;
        this.monitor = monitor;
        operations = ops;
    }

    @Override
    public void run() {
        System.out.println("\tArriba el consumidor " + id + " i farà " + operations + " consumicions");
        // Mientras exista al menos un productor y el cliente tenga operaciones
        // que hacer
        if (PRODUCERS > 0 && operations > 0) {
            for (int i = 0; i < operations; i++) {
                try {
                    counter += monitor.agafar(id, counter, operations); //coge un elemento
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                }
            }
        } else {
            if (PRODUCERS == 0) { //si no hay productores imprimimos lo siguiente
                System.out.println("Consumidor " + id + ": Aquí no hi ha ningú per la màquina!!!");
            }
            //Tanto si no hay productores como si no hay consumidores, estos tienen
            //que acabar, y respetando la exclusión mutua para aumentar el número
            //de clientes acabados
            monitor.acaba();
        }
        System.out.println("----------->Se'n va el consumidor " + id + " i queden " + (CONSUMERS - acabats));
    }
}
