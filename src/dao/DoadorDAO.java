package dao;

import bo.Doador;
import java.io.IOException;
import java.util.Optional;

public interface DoadorDAO {
    boolean inserir(Doador doador) throws IOException;

    Optional<Doador> buscarPorEmail(String email) throws IOException;
}
