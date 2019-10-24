
function confirmUserDeletion() {
    document.getElementById('user-deletion-modal').classList.add('is-visible');
}

function closeModal() {
    document
        .getElementById('modal-close-button')
            .parentElement
                .parentElement
                    .parentElement.classList.remove('is-visible');
}

function confirmModal(uuid) {
    var xhr = new XMLHttpRequest();
    xhr.open('DELETE', '/users/'+uuid, false);
    xhr.onreadystatechange = function() {
        if (this.readyState === XMLHttpRequest.DONE && this.status === 200) {
            window.location = xhr.getResponseHeader('Location');
        }
    }
    xhr.send();
}