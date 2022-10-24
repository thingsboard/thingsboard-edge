## Pull Request description

Put your PR description here instead of this sentence.   

## General checklist

- [ ] This PR has the corresponding CE PR or contains unique code changes related to PE only (does not require same changes in CE).
- [ ] You have reviewed the guidelines [document](https://docs.google.com/document/d/1wqcOafLx5hth8SAg4dqV_LV3un3m5WYR8RdTJ4MbbUM/edit?usp=sharing).
- [ ] PR name contains fix version. For example, "[3.3.4] Hotfix of some UI component" or "[3.4] New Super Feature".
- [ ] The [milestone](https://docs.github.com/en/issues/using-labels-and-milestones-to-track-work/about-milestones) is specified and corresponds to fix version.
- [ ] Description references specific [issue](https://github.com/thingsboard/thingsboard/issues).
- [ ] Description contains human-readable scope of changes.
- [ ] Description contains brief notes about what needs to be added to the documentation.
- [ ] No merge conflicts, commented blocks of code, code formatting issues.
- [ ] Changes are backward compatible or upgrade script is provided.
  
## Front-End feature checklist

- [ ] Screenshots with affected component(s) are added. The best option is to provide 2 screens: before and after changes;
- [ ] If you change the widget or other API, ensure it is backward-compatible or upgrade script is present.
- [ ] Ensure new API is documented [here](https://github.com/thingsboard/thingsboard-pe-ui-help)

## Back-End feature checklist

- [ ] Added corresponding unit and/or integration test(s). Provide written explanation in the PR description if you have failed to add tests.
- [ ] If new dependency was added: the dependency tree is checked for conflicts.
- [ ] If new service was added: the service is marked with corresponding @TbCoreComponent, @TbRuleEngineComponent, @TbTransportComponent, etc.
- [ ] If new REST API was added: the RestClient.java was updated, issue for [Python REST client](https://github.com/thingsboard/thingsboard-python-rest-client) is created.



