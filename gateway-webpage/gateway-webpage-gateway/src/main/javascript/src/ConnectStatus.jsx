import React, { useState, useEffect, useRef, useCallback } from 'react';
import Select from 'react-select';

const ConnectOverview = () => {
    const [options, setOptions] = useState([]);
    const [features, setFeatures] = useState([]);
    const [installDisabled, setInstallDisabled] = useState(true);
    const [refreshOptions, setRefreshOptions] = useState(false);
    const [file, setFile] = useState(null);
    const fileInputRef = useRef(null); // Ref for the file input element

    // Handle file selection
    const handleFileChange = (event) => {
        // Get the selected file
        const selectedFile = event.target.files[0];
        if (selectedFile) {
            setFile(selectedFile);
        }
    };

    // Handle file upload with useCallback to avoid re-creation on every render
    const handleFileUpload = useCallback(() => {
        if (file) {
            // Generate confirmation message
            const confirmationMessage = `Are you sure you want to install the file "${file.name}"?`;

            // Display confirmation dialog
            if (window.confirm(confirmationMessage)) {
                // Proceed with file upload
                console.log('Uploading file:', file.name);

                fetch(`/data/hce/installFile`, {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: {
                        'Accept': 'application/zip',
                    },
                    body: file
                })
                .then(response => response.json())
                .then((data) => {
                    console.log('Upload successful', data);
                    alert("File uploaded successfully");
                    setFile(null);
                    if (fileInputRef.current) {
                        fileInputRef.current.value = ''; // Clear the file input value
                    }
                })
                .catch((error) => {
                    console.error('Upload failed', error);
                    alert("File upload failed");
                    setFile(null);
                    if (fileInputRef.current) {
                        fileInputRef.current.value = ''; // Clear the file input value
                    }
                });
            } else {
                console.log("File upload cancelled");
            }
        } else {
            console.log('No file selected');
        }
    }, [file]); // Dependency on file

    // Function to fetch options
    const fetchOptions = () => {
        fetch(`/data/hce/activeFeatures`, {
            method: 'GET',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json'
            }
        })
        .then(response => response.json())
        .then(json => {
            if (json.availableFeatures) {
                console.log("activeFeatures JSON Response: ");
                console.log(json.availableFeatures);
                const formattedOptions = json.availableFeatures.map(name => ({
                    value: name,
                    label: name
                }));

                setOptions(formattedOptions);

                // Disable the Install button if there are no options available
                setInstallDisabled(formattedOptions.length === 0);
            } else {
                throw new Error("Invalid features array retrieved from config.modules");
            }
        })
        .catch(error => {
            console.error('Error fetching options:', error);
        });
    };

    useEffect(() => {
        // Fetch options when the component mounts or when refreshOptions changes
        fetchOptions();
    }, [refreshOptions]);

    const handleChange = (selectedFeatures) => {
        setFeatures(selectedFeatures || []);
        setInstallDisabled(selectedFeatures.length === 0); // Update button state based on selectedFeatures
    };

    const install = () => {
        console.log("Selected features: ");
        let params = features.map((a) => a.value);
        console.log(params);

        // Convert params array to a query string
        let queryString = `?params=${params.join(',')}`;

        fetch(`/data/hce/install/${queryString}`, {
            method: 'GET',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json'
            }
        })
        .then(response => response.json())
        .then(json => {
            console.log("install JSON Response: ");
            console.log(json);
        });

        setFeatures([]);
        setRefreshOptions(prev => !prev); // Toggle refreshOptions to retrigger fetchOptions
    };

    const resetFeatures = () => {
        fetch(`/data/hce/resetFeatures`, {
            method: 'GET',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json'
            }
        });
        window.location.reload();
    }

    const showConfirmation = () => {
        console.log("Install clicked");

        // Generate confirmation message
        let confirmationMessage = "Are you sure you want to install ";
        if (features.length === 1) {
            confirmationMessage += `${features[0].label}?`;
        } else if (features.length > 1) {
            const featureLabels = features.map(feature => feature.label);
            const lastFeature = featureLabels.pop(); // Remove last element from array

            confirmationMessage += featureLabels.join(', ') + ` and ${lastFeature}?`;
        } else {
            confirmationMessage += "these features?";
        }

        // Display confirmation dialog
        if (window.confirm(confirmationMessage)) {
            install();
            alert("Installed");
        } else {
            console.log("Install cancelled");
        }
    };

    return (
        <div>
            Select Module:
            <Select
                options={options}
                onChange={handleChange}
                value={features}
                isMulti
            />
            <button
                style={{
                    padding: '10px 20px',
                    backgroundColor: installDisabled ? '#cccccc' : '#2199e8', // Grey if disabled
                    color: 'white',
                    border: 'none',
                    borderRadius: '5px',
                    cursor: installDisabled ? 'not-allowed' : 'pointer', // Change cursor if disabled
                    fontSize: '16px',
                    fontWeight: 'bold'
                }}
                onClick={showConfirmation}
                disabled={installDisabled} // Disable button when features are empty
            >
                Install
            </button>
            <button
                style={{
                    padding: '10px 20px',
                    backgroundColor: '#2199e8',
                    color: 'white',
                    border: 'none',
                    borderRadius: '5px',
                    cursor: 'pointer',
                    fontSize: '16px',
                    fontWeight: 'bold'
                }}
                onClick={resetFeatures}
            >
                Reset
            </button>
            or Upload module file
            <div id="file-upload-container">
                <input
                    type="file"
                    ref={fileInputRef} // Attach ref to file input
                    onChange={handleFileChange}
                />
                <button 
                    style={{
                        padding: '10px 20px',
                        backgroundColor: '#2199e8',
                        color: 'white',
                        border: 'none',
                        borderRadius: '5px',
                        cursor: 'pointer',
                        fontSize: '16px',
                        fontWeight: 'bold'
                    }}
                    onClick={handleFileUpload} 
                >
                    Upload File
                </button>
                {file && <p style={{marginTop: '10px', fontSize: '16px'}}>Selected file: {file.name}</p>}
            </div>
        </div>
    );
};

export default ConnectOverview;