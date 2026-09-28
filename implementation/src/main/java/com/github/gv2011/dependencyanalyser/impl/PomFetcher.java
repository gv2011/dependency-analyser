package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.util.ex.Exceptions.call;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;

import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.Repository;
import com.github.gv2011.util.icol.IList;

/**
 * Fetches a pom from the local repository or, if missing there, from remote
 * repositories, in-process via Maven Resolver.
 *
 * <p>The RepositorySystem and its session are created once per instance and
 * reused: creating them is the expensive part, a single resolution is cheap.
 * The session is configured from settings.xml (see {@link SettingsReader}) the
 * same way Maven 3.9's own DefaultRepositorySystemSessionFactory does it:
 * local repository, offline flag, mirrors, active proxies, server credentials.
 */
final class PomFetcher {

  private static final RemoteRepository CENTRAL =
    new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build()
  ;

  private final RepositorySystem system;
  private final RepositorySystemSession session;

  PomFetcher() {
    final Settings settings = new SettingsReader().read();
    system = new RepositorySystemSupplier().get();
    session = createSession(system, settings);
  }

  /**
   * @param repositories consulted before Central, in this order.
   */
  String fetchPomContent(final MavenCoordinates coordinates, final IList<Repository> repositories) {
    return call(() -> Files.readString(fetchPom(coordinates, repositories), StandardCharsets.UTF_8));
  }

  /**
   * @param repositories consulted before Central, in this order.
   * @return the pom file in the local repository
   */
  Path fetchPom(final MavenCoordinates coordinates, final IList<Repository> repositories) {
    if(coordinates.identity().classifier().isPresent() || !coordinates.identity().type().equals("pom")) {
      throw new IllegalArgumentException("Not the coordinates of a pom: " + coordinates);
    }
    final ArtifactRequest request = new ArtifactRequest(
      new DefaultArtifact(
        coordinates.identity().groupId(),
        coordinates.identity().artifactId(),
        "",
        "pom",
        coordinates.version().toString()
      ),
      // Applies mirrors, proxies and credentials from the session.
      system.newResolutionRepositories(session, remoteRepositories(repositories)),
      null
    );
    return call(() -> system.resolveArtifact(session, request)).getArtifact().getFile().toPath();
  }

  private static List<RemoteRepository> remoteRepositories(final IList<Repository> repositories) {
    return Stream
      .concat(
        ( repositories.stream()
          .map(rep -> new RemoteRepository.Builder(rep.id().toString(), "default", rep.url().toString()).build())
        ),
        ( repositories.stream().anyMatch(rep -> rep.id().toString().equals(CENTRAL.getId()))
          ? Stream.empty()
          : Stream.of(CENTRAL)
        )
      )
      .toList()
    ;
  }

  private static RepositorySystemSession createSession(final RepositorySystem system, final Settings settings) {
    final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    session.setOffline(settings.isOffline());
    session.setLocalRepositoryManager(
      system.newLocalRepositoryManager(session, new LocalRepository(settings.getLocalRepository()))
    );
    session.setMirrorSelector(mirrorSelector(settings));
    session.setProxySelector(proxySelector(settings));
    session.setAuthenticationSelector(authenticationSelector(settings));
    session.setReadOnly();
    return session;
  }

  private static DefaultMirrorSelector mirrorSelector(final Settings settings) {
    final DefaultMirrorSelector selector = new DefaultMirrorSelector();
    settings.getMirrors().forEach(m -> selector.add(
      m.getId(), m.getUrl(), m.getLayout(), false, m.isBlocked(), m.getMirrorOf(), m.getMirrorOfLayouts()
    ));
    return selector;
  }

  private static DefaultProxySelector proxySelector(final Settings settings) {
    final DefaultProxySelector selector = new DefaultProxySelector();
    settings.getProxies().stream().filter(p -> p.isActive()).forEach(p -> selector.add(
      new org.eclipse.aether.repository.Proxy(
        p.getProtocol(),
        p.getHost(),
        p.getPort(),
        new AuthenticationBuilder().addUsername(p.getUsername()).addPassword(p.getPassword()).build()
      ),
      p.getNonProxyHosts()
    ));
    return selector;
  }

  private static DefaultAuthenticationSelector authenticationSelector(final Settings settings) {
    final DefaultAuthenticationSelector selector = new DefaultAuthenticationSelector();
    settings.getServers().forEach(s -> selector.add(
      s.getId(),
      new AuthenticationBuilder()
        .addUsername(s.getUsername())
        .addPassword(s.getPassword())
        .addPrivateKey(s.getPrivateKey(), s.getPassphrase())
        .build()
    ));
    return selector;
  }

}
