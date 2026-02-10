package com.ms.usuario.business;

import com.ms.usuario.business.converter.UsuarioConverter;
import com.ms.usuario.business.dto.UsuarioDTO;
import com.ms.usuario.infrastructure.entity.Usuario;
import com.ms.usuario.infrastructure.exceptions.ConflictException;
import com.ms.usuario.infrastructure.exceptions.ResourceNotFoundException;
import com.ms.usuario.infrastructure.repository.UsuarioRepository;
import com.ms.usuario.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final UsuarioConverter usuarioConverter;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    public UsuarioDTO salvaUsuario(UsuarioDTO usuarioDTO){
        emailExiste(usuarioDTO.getEmail());
        usuarioDTO.setSenha(passwordEncoder.encode(usuarioDTO.getSenha()));
        Usuario usuario = usuarioConverter.paraUsuario(usuarioDTO);
        usuario = usuarioRepository.save(usuario);
        return usuarioConverter.paraUsuarioDTO(usuario);
    }

    //este método lança a exceção caso o email já esteja cadastrado
    public void emailExiste(String email){
        try{
            boolean existe = verificaEmailExistente(email);
            if(existe){
                throw new ConflictException("Email já cadastrado! " + email);
            }
        } catch (ConflictException e){
            throw new ConflictException("Email já cadastrado! ", e.getCause());
        }
    }

    //este método verifica se o email já existe no BD
    public boolean verificaEmailExistente(String email){
        return usuarioRepository.existsByEmail(email);
    }

    public Usuario buscaUsuarioPorEmail(String email){
        return usuarioRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("Email não encontrado " + email));
    }

    public void deletaUsuarioPorEmail(String email){
        usuarioRepository.deleteByEmail(email);
    }

    public UsuarioDTO atualizaDadosUsuario(String token, UsuarioDTO dto){

        ////busca email do usuario através do token
        String email = jwtUtil.extairEmailToken(token.substring(7));

        //Criptografia de senha
        dto.setSenha(dto.getSenha() != null ? passwordEncoder.encode(dto.getSenha()) : null);

        //busca os dados do usuario no banco de dados
        Usuario usuarioEntity =
                usuarioRepository.findByEmail(email).orElseThrow(() ->
                        new ResourceNotFoundException("Email não localizado")); //retorna exceçãocaso o email não exista

        //mesclou os dados que recebemos na requisição DTO com os dados do banco de dados
        Usuario usuario = usuarioConverter.updateUsuario(dto, usuarioEntity);


        //salvou os dados do usuario convertido e depois pegou o retorno e converteu para UsuarioDto
        return usuarioConverter.paraUsuarioDTO(usuarioRepository.save(usuario));
    }

}
