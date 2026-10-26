val prepareWeb = tasks.register<Exec>("prepareWeb") {
    description = "Prepare web application"
    group = "Docker"

    workingDir = layout.projectDirectory.dir("web").asFile
    commandLine("npm", "install")
}

val assembleWeb = tasks.register<Exec>("assembleWeb") {
    description = "Assembles web application"
    group = "Docker"

    dependsOn(prepareWeb)

    workingDir = layout.projectDirectory.dir("web").asFile
    commandLine("npm", "run", "build")
}

val buildDockerImage = tasks.register<Exec>("buildDockerImage") {
    description = "Builds docker image and tag"
    group = "Docker"

    dependsOn(assembleWeb)
    commandLine("docker", "build", "--tag", "bbdm", ".")
}

val runDockerContainer = tasks.register<Exec>("runDockerContainer") {
    description = "Runs docker container"
    group = "Docker"

    dependsOn(buildDockerImage)
    commandLine("docker", "run", "--rm", "--publish", "4200:4200", "bbdm")
}

tasks.register("runDocker") {
    description = "Runs docker container"
    group = "Docker"

    dependsOn(runDockerContainer)
}
