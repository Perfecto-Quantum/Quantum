@test
Feature: Google Search

  @test
  Scenario: Search Quantum
    Given I am on Google Search Page
    Then I check window "Google page"
    Then I check region "name=q"
    Then I check region "name=btnK"
    Then I close the eye
    When I search for "dutch shephards"
    #Then I check window "Google page"
    #Then I check region "search.text.box"
    #Then I check region "search.button"
#    When I search for "quantum perfecto"
    Then it should have "Introducing Quantum Framework" in search results
    Then I am on Google Search Page
