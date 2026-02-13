package dev.snowdrop.openrewrite.cli.toolbox;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;

import java.util.List;

/**
 * A ModelResolver that uses the 'maven-resolver' library to find POMs
 * in the local .m2 repository and on Maven Central.
 */
public class RepositoryModelResolver implements ModelResolver {

	private final RepositorySystem repoSystem;
	private final DefaultRepositorySystemSession session;
	private final List<RemoteRepository> repositories;

	public RepositoryModelResolver() {
		this.repoSystem = MavenUtils.createRepositorySystem();
		this.session = MavenUtils.createRepositorySession(this.repoSystem);
		this.repositories = List
				.of(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build());
	}

	// This is a "deep copy" constructor for the resolver
	private RepositoryModelResolver(RepositoryModelResolver original) {
		this.repoSystem = original.repoSystem;
		this.session = original.session;
		this.repositories = original.repositories;
	}

	@Override
	public ModelSource resolveModel(String groupId, String artifactId, String version)
			throws UnresolvableModelException {
		try {
			Artifact pomArtifact = new DefaultArtifact(groupId, artifactId, "pom", version);
			ArtifactRequest request = new ArtifactRequest(pomArtifact, repositories, null);
			ArtifactResult result = repoSystem.resolveArtifact(session, request);
			return new FileModelSource(result.getArtifact().getFile());
		} catch (Exception e) {
			throw new UnresolvableModelException(e.getMessage(), groupId, artifactId, version, e);
		}
	}

	@Override
	public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
		// This resolves the parent POM
		return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
	}

	@Override
	public ModelSource resolveModel(Dependency dependency) throws UnresolvableModelException {
		return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
	}

	// These methods are required by the interface but we don't need them
	// for this simple case.
	@Override
	public void addRepository(Repository repository) throws InvalidRepositoryException {
		// no-op
	}

	@Override
	public void addRepository(Repository repository, boolean replace) throws InvalidRepositoryException {
		// no-op
	}

	@Override
	public ModelResolver newCopy() {
		return new RepositoryModelResolver(this);
	}


	public RepositorySystem getRepoSystem() {
		return repoSystem;
	}

	public DefaultRepositorySystemSession getSession() {
		return session;
	}

	public List<RemoteRepository> getRepositories() {
		return repositories;
	}
}