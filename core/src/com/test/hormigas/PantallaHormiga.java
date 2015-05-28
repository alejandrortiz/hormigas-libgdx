package com.test.hormigas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FillViewport;

import java.util.Random;
import java.util.Vector;

public class PantallaHormiga implements Screen {

    private static final int HORMIGAS_POR_CLIC = 25;
    private final int MARGEN_PLANTAS = 70;

    private Stage stage;
    private FillViewport viewport;

    private SpriteBatch stageBatch;

    private static Vector<MyActor> actores;
    private static Vector<MyActor> pendientesEliminar;

    private Random ran = new Random();

    public PantallaHormiga(HormigasGame game) {
        stageBatch = new SpriteBatch();
        viewport = new FillViewport(Assets.screenWidth, Assets.screenHeight);
        actores = new Vector<>();
        pendientesEliminar = new Vector<>();

        stage = new Stage(viewport, stageBatch);
        Gdx.input.setInputProcessor(stage);

        Image background = new Image(Assets.background);
        background.setSize(Assets.screenWidth, Assets.screenHeight);
        background.setScaling(Scaling.fill);

        stage.addActor(background);

        crearHormigas(1, 2);
        crearHormigas(2, 2);
        crearHormigas(3, 2);
        crearHormigas(4, 2);
        crearHormigas(5, 2);
        crearPlantas(0);
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glClearColor(1, 1, 1, 1);

        act(delta);
        draw();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit();
    }


    public void act(float delta) {
        stage.act(delta);
        processInput();
        detectarColision();
        Gdx.app.log("Numero de hormigas:", "" + actores.size());
    }

    public void draw() {
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
    }

    /**
     * ACCIONES EN PANTALLA
     */

    public void crearHormiga(Vector2 pos) {
        crearHormiga(ran.nextInt(5) + 1, pos);
    }

    public void crearHormiga(int tipo, Vector2 pos) {
        Hormiga h = new Hormiga(tipo, pos.x, pos.y);
        h.setZIndex(10);
        actores.add(h);
        stage.addActor(h);
        h.setZIndex(500);
        h.getPolygon().setPosition(h.getX(), h.getY());
    }

    public void crearHuevo(final int tipo, Vector2 pos) {
        final Huevo huevo = new Huevo(tipo);
        huevo.setPosition(pos.x, pos.y);
        stage.addActor(huevo);
        huevo.setZIndex(1);
        stage.addAction(Actions.delay(Huevo.TIEMPO_ECLOSION,
                        Actions.run(new Runnable() {
                            @Override
                            public void run() {
                                crearHormiga(tipo, new Vector2(huevo.getX(), huevo.getY()));
                                huevo.remove();
                            }
                        })
                )
        );
    }

    public void crearPlanta(Vector2 pos) {
        Planta p = new Planta(pos.x, pos.y);
        actores.add(p);
        stage.addActor(p);
        p.getPolygon().setPosition(p.getX(), p.getY());
    }

    public void crearHormigas(int tipo, int numero) {
        for (int i = 0; i < numero; i++) {
            crearHormiga(tipo, new Vector2(ran.nextFloat() * (Assets.screenWidth - Planta.TAMANO), ran.nextFloat() * (Assets.screenHeight - Planta.TAMANO)));
        }
    }

    public void crearPlantas(int numero) {
        for (int i = 0; i < numero; i++) {
            crearPlanta(new Vector2(ran.nextFloat() * (Assets.screenWidth - MARGEN_PLANTAS * 2 - Planta.TAMANO) + MARGEN_PLANTAS, ran.nextFloat() * (Assets.screenHeight - MARGEN_PLANTAS * 2 - Planta.TAMANO) + MARGEN_PLANTAS));
        }
    }

    public void detectarColision() {

        for (MyActor act : pendientesEliminar) {
            actores.remove(act);
            act.remove();
        }

        pendientesEliminar.clear();

        for (MyActor act1 : actores) {
            if (act1 instanceof Hormiga && ((Hormiga) act1).isChocada())
                continue;
            for (MyActor act2 : actores) {
                if (act2 instanceof Hormiga && ((Hormiga) act2).isChocada())
                    continue;
                if (act1 != act2 && (act2.getPolygon().contains(act1.getPolygon().getX(), act1.getPolygon().getY())
                        || act2.getPolygon().contains(act1.getPolygon().getX() + act1.getTamano(), act1.getPolygon().getY())
                        || act2.getPolygon().contains(act1.getPolygon().getX() + act1.getTamano(), act1.getPolygon().getY() + act1.getTamano())
                        || act2.getPolygon().contains(act1.getPolygon().getX(), act1.getPolygon().getY() + act1.getTamano()))) {
                    choque(act1, act2);
                }
            }
        }
    }

    public void choque(final MyActor actor1, final MyActor actor2) {
        if (actor1 instanceof Hormiga && actor2 instanceof Hormiga)
            choqueEntreHormigas((Hormiga) actor1, (Hormiga) actor2);
        else if (actor1 instanceof Hormiga && actor2 instanceof Planta)
            choqueHormigaPlanta((Hormiga) actor1, (Planta) actor2);
        else if (actor1 instanceof Planta && actor2 instanceof Hormiga)
            choqueHormigaPlanta((Hormiga) actor2, (Planta) actor1);
    }

    private void choqueHormigaPlanta(final Hormiga hormiga, final Planta planta) {
        if (!hormiga.isEsAdulta()) {
            hormiga.seguirCreciendo();
            return;
        }

        int pro = ran.nextInt(100) + 1;

        if (!hormiga.isChocada()) {
            hormiga.setChocada(true);
            hormiga.clearActions();
            hormiga.mirar(planta);

            // Regar planta
            if (planta.isViva() && !planta.isComestible() && hormiga.getTipo() != Hormiga.ROJA) {
                if (hormiga.getTipo() == Hormiga.VERDE) {
                    hormiga.pelear();
                    planta.regar();
                    hormiga.regar();
                } else if (hormiga.getTipo() == Hormiga.AZUL && pro >= 1 && pro <= 75) {
                    hormiga.pelear();
                    planta.regar();
                    hormiga.regar();
                } else if (hormiga.getTipo() == Hormiga.NARANJA && pro >= 1 && pro <= 50) {
                    hormiga.pelear();
                    planta.regar();
                    hormiga.regar();
                } else if (hormiga.getTipo() == Hormiga.ROSA && pro >= 1 && pro <= 25) {
                    hormiga.pelear();
                    planta.regar();
                    hormiga.regar();
                } else {
                    hormiga.invertDireccion();
                }

                // Comer planta
            } else if (planta.isComestible() && planta.isViva()) {
                hormiga.pelear();
                planta.comer();
                hormiga.comer();
                // Matar planta
                if (!planta.isComestible()) {
                    planta.matar();
                    stage.addAction(Actions.delay(Hormiga.TIEMPO_PELEA * Hormiga.IMPACTOS_PELEA * 2,
                                    Actions.run(new Runnable() {
                                        @Override
                                        public void run() {
                                            pendientesEliminar.add(planta);
                                        }
                                    })
                            )
                    );
                }

            } else {
                hormiga.invertDireccion();
            }
        }
    }

    private void choqueEntreHormigas(final Hormiga h1, final Hormiga h2) {

        if (!h1.isEsAdulta() || !h2.isEsAdulta()) {
            if (h1.isEsAdulta()) {
                h1.invertDireccion();
                h2.seguirCreciendo();
            } else if (h2.isEsAdulta()) {
                h2.invertDireccion();
                h1.seguirCreciendo();
            } else {
                h1.seguirCreciendo();
                h2.seguirCreciendo();
            }
            return;
        }

        if (h1.isChocada() || h2.isChocada())
            return;
        else {
            h1.setChocada(true);
            h2.setChocada(true);
            h1.clearActions();
            h2.clearActions();
        }

        // Reproducirse
        int pro1 = ran.nextInt(100) + 1;
        int pro2 = ran.nextInt(100) + 1;
        int pp1;
        int pr1;
        int pp2;
        int pr2;

        if (h1.getTipo() == Hormiga.VERDE) {
            pr1 = 20;
            pp1 = 0;
        } else if (h1.getTipo() == Hormiga.NARANJA) {
            pp1 = 40;
            pr1 = 20;
        } else if (h1.getTipo() == Hormiga.ROJA) {
            pp1 = 80;
            pr1 = 20;
        } else if (h1.getTipo() == Hormiga.AZUL) {
            pp1 = 20;
            pr1 = 20;
        } else {
            pp1 = 60;
            pr1 = 20;
        }

        if (h2.getTipo() == Hormiga.VERDE) {
            pr2 = 20;
            pp2 = 0;
        } else if (h2.getTipo() == Hormiga.NARANJA) {
            pp2 = 40;
            pr2 = 20;
        } else if (h2.getTipo() == Hormiga.ROJA) {
            pp2 = 80;
            pr2 = 20;
        } else if (h2.getTipo() == Hormiga.AZUL) {
            pp2 = 20;
            pr2 = 20;
        } else {
            pp2 = 60;
            pr2 = 20;
        }

        if (pro1 >= 1 && pro1 <= pp1 || pro2 >= 1 && pro2 <= pp2) {
            h1.mirar(h2);
            h2.mirar(h1);
            h1.pelear();
            h2.pelear();
            pelearse(h1, h2);
        } else if (pro1 >= pp1 + 1 && pro1 <= pp1 + pr1 || pro2 >= pp2 + 1 && pro2 <= pp2 + pr2) {
            h1.reproducir();
            h2.reproducir();
            h1.mirar(h2);
            h2.mirar(h1);
            h1.pelear();
            h2.pelear();

            Vector2 centroH1 = h1.localToStageCoordinates(new Vector2(h1.getOriginX(), h1.getOriginY()));
            Vector2 centroH2 = h2.localToStageCoordinates(new Vector2(h2.getOriginX(), h2.getOriginY()));

            final Vector2 centroHuevo = new Vector2((centroH1.x + centroH2.x) / 2, (centroH1.y + centroH2.y) / 2);
            stage.addAction(Actions.delay(Hormiga.TIEMPO_PELEA * Hormiga.IMPACTOS_PELEA * 2,
                            Actions.run(new Runnable() {
                                @Override
                                public void run() {
                                    crearHuevo(h1.getTipo(), centroHuevo);
                                }
                            })
                    )
            );

            return;
        } else {
            h1.invertDireccion();
            h2.invertDireccion();
        }
    }

    public void pelearse(Hormiga h1, Hormiga h2) {
        int h1e = h1.getEnergia();
        int h1v = h1.getVictorias();
        int h2e = h2.getEnergia();
        int h2v = h2.getVictorias();

        int h1t;
        int h2t;

        /**
         * Comprueba cual de las dos hormigas tiene más energia.
         * La que tiene más energia es el 100% y la otra sera el porcentaje respecto a la otra.
         * La energia vale un 60%.
         */
        if (h1e > h2e && (h1v != 0 && h2v != 0)) {
            h2e = (h1e / h2e) * 10;
            h1e = 60;
        } else if (h1e > h2e && (h1v != 0 && h2v != 0)) {
            h1e = (h2e / h1e) * 10;
            h2e = 60;
        }

        /**
         * Comprueba cual de las dos hormigas tiene más energia.
         * La que tiene más victorias es el 100% y la otra sera el porcentaje respecto a la otra.
         * La victorias vale un 40%.
         */
        if (h1v > h2v && (h1v != 0 && h2v != 0)) {
            h2v = (h1v / h2v) * 10;
            h1v = 40;
        } else if (h1v < h2v && (h1v != 0 && h2v != 0)) {
            h1v = (h2v / h1v) * 10;
            h2v = 40;
        } else {
            h2v = 0;
            h1v = 0;
        }

        /**
         * Se suman ambos resultados. De ambas hormigas.
         */
        h1t = h1e + h1v;
        h2t = h2e + h2v;

        /**
         * La que tenga mas puntuación que la otra ganará.
         * Y por lo tanto se hace con la mitad de la energia de la otra.
         */
        if (h1t > h2t) {
            h1.ganarPelea(h2.getEnergia() / 2);
            h2.perderPelea();
        } else {
            h2.ganarPelea(h1.getEnergia() / 2);
            h1.perderPelea();
        }
    }

    private void processInput() {

        // Se ejecuta cuando le das ESC (ordenador) y Atras (movil).
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            //TODO cerrar aplicaciones
        }

        Vector2 touchPoint = new Vector2();

        stage.getViewport().unproject(touchPoint.set(Gdx.input.getX(), Gdx.input.getY()));

        if (Gdx.input.isKeyJustPressed(Input.Keys.H) || (Gdx.input.justTouched() && Gdx.input.isButtonPressed(Input.Buttons.LEFT))) {

            //TODO añadir hormiga
            for (int i = 0; i < HORMIGAS_POR_CLIC; i++) {
                Random ran = new Random();
                Hormiga h = new Hormiga(ran.nextInt(5) + 1, touchPoint.x, touchPoint.y);
                actores.add(h);
                stage.addActor(h);
                h.getPolygon().setPosition(h.getX(), h.getY());
            }
        }


        if (Gdx.input.isKeyJustPressed(Input.Keys.P) || (Gdx.input.justTouched() && Gdx.input.isButtonPressed(Input.Buttons.RIGHT))) {

            //TODO añadir Planta
            Planta p = new Planta(touchPoint.x - Planta.TAMANO / 2, touchPoint.y - Planta.TAMANO / 2);
            actores.add(p);
            stage.addActor(p);
            p.getPolygon().setPosition(p.getX(), p.getY());
        }
    }

    /**
     * GETTERS AND SETTERS
     */

    public static Vector<MyActor> getActores() {
        return actores;
    }
}
