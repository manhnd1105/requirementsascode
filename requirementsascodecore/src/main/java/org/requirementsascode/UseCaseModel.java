package org.requirementsascode;

import static org.requirementsascode.ModelElementContainer.findModelElement;
import static org.requirementsascode.ModelElementContainer.getModelElements;
import static org.requirementsascode.ModelElementContainer.hasModelElement;
import static org.requirementsascode.ModelElementContainer.saveModelElement;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.requirementsascode.exception.NoSuchElementInModel;

/**
 * A use case model is a container for use cases and their associated actors. It is used to
 * configure a {@link UseCaseModelRunner}.
 *
 * @author b_muth
 */
public class UseCaseModel {
  private Map<String, Actor> nameToActorMap;
  private Map<String, UseCase> nameToUseCaseMap;
  private Actor userActor;
  private Actor systemActor;

  UseCaseModel() {
    this.nameToActorMap = new HashMap<>();
    this.nameToUseCaseMap = new HashMap<>();
    this.userActor = newActor("user");
    this.systemActor = newActor("system");
  }

  /**
   * Checks whether this model contains the specified actor.
   *
   * @param actorName the name of the actor whose existence to check
   * @return true if this model contains the specified actor, false otherwise
   */
  public boolean hasActor(String actorName) {
    boolean hasActor = hasModelElement(actorName, nameToActorMap);
    return hasActor;
  }

  /**
   * Checks whether this model contains the specified use case.
   *
   * @param useCaseName the name of the use case whose existence to check
   * @return true if this model contains the specified use case, false otherwise
   */
  public boolean hasUseCase(String useCaseName) {
    boolean hasUseCase = hasModelElement(useCaseName, nameToUseCaseMap);
    return hasUseCase;
  }

  Actor newActor(String actorName) {
    Actor actor = new Actor(actorName, this);
    saveModelElement(actor, nameToActorMap);
    return actor;
  }

  UseCase newUseCase(String useCaseName) {
    UseCase useCase = new UseCase(useCaseName, this);
    saveModelElement(useCase, nameToUseCaseMap);
    return useCase;
  }

  /**
   * Finds the actor with the specified name, contained in this model.
   *
   * @param actorName the name of the actor to look for
   * @return the actor if found, or else an empty optional
   * @throws NoSuchElementInModel if no actor with the specified actorName is found in the model
   */
  public Actor findActor(String actorName) {
    Actor actor = findModelElement(actorName, nameToActorMap);
    return actor;
  }

  /**
   * Finds the use case with the specified name, contained in this model.
   *
   * @param useCaseName the name of the use case to look for
   * @return the use case if found, or else an empty optional
   * @throws NoSuchElementInModel if no use case with the specified useCaseName is found in the
   *     model
   */
  public UseCase findUseCase(String useCaseName) {
    UseCase useCase = findModelElement(useCaseName, nameToUseCaseMap);
    return useCase;
  }

  /**
   * Returns the actors contained in this use case model. Do not modify that collection directly,
   * use {@link #newActor(String)}.
   *
   * @return the actors
   */
  public Collection<Actor> getActors() {
    Collection<Actor> modifiableActors = getModelElements(nameToActorMap);
    return Collections.unmodifiableCollection(modifiableActors);
  }

  /**
   * Returns the use cases contained in this use case model.
   *
   * @return the use cases
   */
  public Collection<UseCase> getUseCases() {
    Collection<UseCase> modifiableUseCases = getModifiableUseCases();
    return Collections.unmodifiableCollection(modifiableUseCases);
  }

  Collection<UseCase> getModifiableUseCases() {
    return getModelElements(nameToUseCaseMap);
  }

  /**
   * Returns the use case steps of use cases contained in this use case model.
   *
   * @return the use steps
   */
  public Collection<Step> getSteps() {
    Set<Step> modifiableSteps = getModifiableSteps();
    return Collections.unmodifiableCollection(modifiableSteps);
  }

  Set<Step> getModifiableSteps() {
    return getModifiableUseCases()
        .stream()
        .map(useCase -> useCase.getModifiableSteps())
        .flatMap(steps -> steps.stream())
        .collect(Collectors.toSet());
  }

  /**
   * Returns the actor representing the default user.
   *
   * @return the user actor
   */
  public Actor getUserActor() {
    return userActor;
  }

  /**
   * Returns the actor representing the system.
   *
   * @return the user actor
   */
  public Actor getSystemActor() {
    return systemActor;
  }
}
